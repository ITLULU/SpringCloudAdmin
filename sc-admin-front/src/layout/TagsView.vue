<template>
  <div class="tags-view-container">
    <el-scrollbar class="tags-view-wrapper">
      <router-link
        v-for="tag in appStore.visitedViews"
        :key="tag.path"
        :to="tag.path"
        class="tags-view-item"
        :class="{ active: isActive(tag) }"
      >
        {{ tag.title }}
        <el-icon
          v-if="!isAffix(tag)"
          class="close-icon"
          @click.prevent.stop="closeTag(tag)"
        >
          <Close />
        </el-icon>
      </router-link>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { Close } from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()

function isActive(tag: { path: string }) {
  return tag.path === route.path
}

function isAffix(tag: { path: string }) {
  return tag.path === '/dashboard'
}

function addTag() {
  if (route.name && route.meta?.title) {
    appStore.addVisitedView({
      path: route.path,
      title: route.meta.title as string,
      name: route.name as string
    })
  }
}

function closeTag(tag: { path: string }) {
  appStore.removeVisitedView(tag.path)
}

watch(
  () => route.path,
  () => {
    addTag()
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: $tagsViewHeight;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.04);
}

.tags-view-wrapper {
  white-space: nowrap;
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  height: 26px;
  line-height: 26px;
  margin: 4px 0 4px 6px;
  border: 1px solid #d8dce5;
  border-radius: 3px;
  font-size: 12px;
  color: #495060;
  text-decoration: none;
  cursor: pointer;

  &.active {
    background-color: $primaryColor;
    color: #fff;
    border-color: $primaryColor;
  }

  .close-icon {
    margin-left: 4px;
    font-size: 12px;
    border-radius: 50%;

    &:hover {
      background-color: rgba(0, 0, 0, 0.15);
      color: #fff;
    }
  }
}
</style>
