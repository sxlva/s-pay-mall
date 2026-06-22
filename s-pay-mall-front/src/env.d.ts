/**
 * Vue 模块类型声明
 *
 * @author 傅崇睿
 */
declare module '*.vue' {
    import type { DefineComponent } from 'vue'
    const component: DefineComponent<{}, {}, any>
    export default component
}