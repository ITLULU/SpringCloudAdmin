<template>
  <div class="sidebar-container" :class="{ 'is-collapsed': appStore.sidebarCollapsed }">
    <div class="sidebar-logo">
      <img src="/favicon.svg" alt="logo" class="logo-img" />
      <span v-show="!appStore.sidebarCollapsed" class="logo-text">Admin</span>
    </div>
    <el-scrollbar>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        router
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <!-- 单级菜单 -->
          <template v-if="!route.children || route.children.length === 1">
            <el-menu-item
              v-if="route.children && route.children.length === 1"
              :index="route.redirect as string || route.path + '/' + route.children[0].path"
            >
              <el-icon v-if="route.children[0].meta?.icon">
                <component :is="route.children[0].meta.icon" />
              </el-icon>
              <template #title>{{ route.children[0].meta?.title }}</template>
            </el-menu-item>
          </template>
          <!-- 多级菜单 -->
          <el-sub-menu v-else :index="route.path">
            <template #title>
              <el-icon v-if="route.meta?.icon">
                <component :is="route.meta.icon" />
              </el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in route.children"
              :key="child.path"
              :index="route.path + '/' + child.path"
            >
              <el-icon v-if="child.meta?.icon">
                <component :is="child.meta.icon" />
              </el-icon>
              <template #title>{{ child.meta?.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)

const menuRoutes = computed(() => {
  return router.options.routes.filter((r) => !r.meta?.hidden)
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: $sidebarWidth;
  background-color: $sidebarBg;
  transition: width 0.3s;
  z-index: 1001;
  overflow: hidden;

  &.is-collapsed {
    width: $sidebarCollapsedWidth;
  }
}

.sidebar-logo {
  height: $headerHeight;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background-color: #2b3a4c;
  overflow: hidden;
}

.logo-img {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.logo-text {
  margin-left: 10px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.el-menu {
  border-right: none;
  width: 100%;
}
</style>
