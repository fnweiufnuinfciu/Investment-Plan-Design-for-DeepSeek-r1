#!/usr/bin/env python3
"""
FinBERT sentence-level subjective/objective classifier.
Reads JSON from stdin, writes JSON to stdout.

Input:  {"text": "report text...", "ticker": "AAPL"}
Output: {"objective_ratio": 0.65, "subjective_ratio": 0.35,
         "avg_confidence": 0.87, "sentence_count": 12,
         "classifications": [{"sentence": "...", "label": "objective", "score": 0.92}, ...]}
"""

import json
import re
import sys


def split_sentences(text):
    """Split Chinese/English financial text into sentences."""
    # Normalize whitespace
    text = re.sub(r'\s+', ' ', text.strip())
    # Split on Chinese/English sentence boundaries
    # Chinese: 。！？\n  English: . ! ?
    pattern = r'(?<=[。！？\.\!\?\n])(?=[^\s\.\,\;\:\!\?\"\'\)\]\}\)])'
    raw = re.split(pattern, text)
    # Filter empty and very short
    sentences = []
    for s in raw:
        s = s.strip()
        if len(s) >= 3:
            sentences.append(s)
    return sentences


def load_model():
    """Load FinBERT model for sequence classification."""
    try:
        from transformers import AutoTokenizer, AutoModelForSequenceClassification
        import torch

        model_name = "ProsusAI/finbert"
        tokenizer = AutoTokenizer.from_pretrained(model_name)
        model = AutoModelForSequenceClassification.from_pretrained(model_name)
        model.eval()

        if torch.cuda.is_available():
            model = model.to("cuda")

        return tokenizer, model, torch.device("cuda" if torch.cuda.is_available() else "cpu")
    except Exception as e:
        print(json.dumps({"error": f"Model load failed: {e}", "fallback": True}),
              file=sys.stderr)
        return None, None, None


def classify_sentences(sentences, tokenizer, model, device):
    """Classify each sentence as objective/subjective using FinBERT."""
    import torch

    results = []
    batch_size = 32
    id2label = {0: "positive", 1: "negative", 2: "neutral"}

    for i in range(0, len(sentences), batch_size):
        batch = sentences[i:i + batch_size]
        try:
            inputs = tokenizer(
                batch,
                padding=True,
                truncation=True,
                max_length=512,
                return_tensors="pt"
            ).to(device)

            with torch.no_grad():
                outputs = model(**inputs)
                probs = torch.softmax(outputs.logits, dim=-1)

            # FinBERT outputs sentiment (positive/negative/neutral), not subj/obj.
            # For financial texts, we map: neutral sentiment tends to be objective,
            # positive/negative sentiment tends to be subjective (opinion).
            for j, sent in enumerate(batch):
                prob_dist = probs[j].cpu().tolist()
                max_idx = prob_dist.index(max(prob_dist))
                sentiment = id2label[max_idx]
                max_score = prob_dist[max_idx]

                # Higher neutral score → more objective
                # Higher pos/neg score → more subjective
                neutral_score = prob_dist[2]
                is_objective = neutral_score >= 0.45
                label = "objective" if is_objective else "subjective"

                results.append({
                    "sentence": sent,
                    "label": label,
                    "confidence": round(max_score, 4),
                    "neutral_score": round(neutral_score, 4)
                })
        except Exception as e:
            for sent in batch:
                results.append({
                    "sentence": sent,
                    "label": "unknown",
                    "confidence": 0.0,
                    "neutral_score": 0.0,
                    "error": str(e)
                })

    return results


def process(text, tokenizer, model, device):
    """Full pipeline: split → classify → aggregate."""
    sentences = split_sentences(text)

    if not sentences:
        return {
            "objective_ratio": 0.5,
            "subjective_ratio": 0.5,
            "avg_confidence": 0.5,
            "sentence_count": 0,
            "classifications": [],
            "note": "No valid sentences found"
        }

    if tokenizer is None or model is None:
        # Model unavailable — return estimate based on sentence structure
        return heuristic_fallback(sentences)

    classifications = classify_sentences(sentences, tokenizer, model, device)

    obj_count = sum(1 for c in classifications if c["label"] == "objective")
    total = len(classifications)
    avg_conf = sum(c["confidence"] for c in classifications) / max(total, 1)

    return {
        "objective_ratio": round(obj_count / total, 4),
        "subjective_ratio": round((total - obj_count) / total, 4),
        "avg_confidence": round(avg_conf, 4),
        "sentence_count": total,
        "classifications": classifications
    }


def heuristic_fallback(sentences):
    """Heuristic: sentences with numbers/data keywords → likely objective."""
    data_keywords = ['增长', '下降', '亿元', '万元', '%', '同比', '环比',
                     '收入', '利润', '毛利率', '净利率', 'ROE', 'EPS',
                     'PE', 'PB', '市值', '营收', '净利', 'revenue',
                     'profit', 'margin', 'growth', 'billion', 'million',
                     '亿', '万', 'Q1', 'Q2', 'Q3', 'Q4', 'H1', 'H2']

    results = []
    obj_count = 0
    for sent in sentences:
        score = sum(1 for kw in data_keywords if kw.lower() in sent.lower())
        is_obj = score >= 2
        if is_obj:
            obj_count += 1
        results.append({
            "sentence": sent,
            "label": "objective" if is_obj else "subjective",
            "confidence": min(0.7, 0.5 + 0.1 * score),
            "neutral_score": 0.7 if is_obj else 0.3,
            "fallback": True
        })

    total = len(results)
    avg_conf = sum(c["confidence"] for c in results) / max(total, 1)
    return {
        "objective_ratio": round(obj_count / total, 4) if total > 0 else 0.5,
        "subjective_ratio": round((total - obj_count) / total, 4) if total > 0 else 0.5,
        "avg_confidence": round(avg_conf, 4),
        "sentence_count": total,
        "classifications": results,
        "note": "Heuristic fallback — FinBERT model not loaded"
    }


def main():
    # Load model once at startup
    tokenizer, model, device = load_model()
    model_loaded = tokenizer is not None and model is not None

    if model_loaded:
        print(json.dumps({"status": "ready", "model": "ProsusAI/finbert"}),
              file=sys.stderr)
    else:
        print(json.dumps({"status": "ready", "mode": "heuristic_fallback"}),
              file=sys.stderr)
    sys.stderr.flush()

    # Process inputs line by line (each line = one JSON request)
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue

        try:
            req = json.loads(line)
            text = req.get("text", "")
            result = process(text, tokenizer, model, device)
            print(json.dumps(result, ensure_ascii=False))
            sys.stdout.flush()
        except Exception as e:
            error_result = {"error": str(e), "objective_ratio": 0.5,
                            "subjective_ratio": 0.5, "fallback": True}
            print(json.dumps(error_result, ensure_ascii=False))
            sys.stdout.flush()


if __name__ == "__main__":
    main()
