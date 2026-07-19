<template>
  <div class="app-wrapper" :class="{ 'sidebar-collapsed': appStore.sidebarCollapsed }">
    <Sidebar />
    <div class="main-container">
      <Header />
      <TagsView />
      <div class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import TagsView from './TagsView.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style lang="scss" scoped>
.app-wrapper {
  height: 100%;
}

.sidebar-collapsed {
  .main-container {
    margin-left: $sidebarCollapsedWidth;
  }
}

.main-container {
  margin-left: $sidebarWidth;
  transition: margin-left 0.3s;
}

.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
