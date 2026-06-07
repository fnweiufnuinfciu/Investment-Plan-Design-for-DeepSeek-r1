<template>
  <div class="app-card" :style="bodyStyle">
    <div v-if="$slots.header || title" class="app-card-header">
      <h2 v-if="title">{{ title }}</h2>
      <slot name="header" />
    </div>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  bodyStyle: { type: String, default: '' },
})
</script>

<style scoped>
.app-card {
  background: var(--card-bg, #fafaf9);
  border: 1px solid var(--card-border, #e7e5e0);
  border-radius: var(--radius, 10px);
  box-shadow: 0 1px 0 var(--card-border, #e7e5e0), 0 2px 8px rgba(0,0,0,0.04);
  padding: 24px;
  transition: box-shadow 320ms cubic-bezier(0.4,0,0.2,1);
}
.app-card:hover {
  box-shadow: 0 2px 0 var(--card-border, #e7e5e0), 0 4px 20px rgba(0,0,0,0.08);
}
.app-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border, #e4e4e0);
}
.app-card-header h2 {
  font-family: var(--font-display, serif);
  font-size: 1rem;
  font-weight: 700;
  margin: 0;
  color: var(--text, #1c1c1e);
  letter-spacing: 0.03em;
}
</style>
