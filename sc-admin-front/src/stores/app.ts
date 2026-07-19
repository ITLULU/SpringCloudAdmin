import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const activeMenu = ref('')

  // 已打开的标签页
  const visitedViews = ref<{ path: string; title: string; name?: string }[]>([])

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function addVisitedView(view: { path: string; title: string; name?: string }): void {
    if (visitedViews.value.some((v) => v.path === view.path)) return
    visitedViews.value.push(view)
  }

  function removeVisitedView(path: string): void {
    const index = visitedViews.value.findIndex((v) => v.path === path)
    if (index > -1) {
      visitedViews.value.splice(index, 1)
    }
  }

  return {
    sidebarCollapsed,
    activeMenu,
    visitedViews,
    toggleSidebar,
    addVisitedView,
    removeVisitedView
  }
})
