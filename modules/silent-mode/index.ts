// Re-export the native module. On web, it will be resolved to SilentModeModule.web.ts
// and on native platforms to SilentModeModule.ts
export { default } from './src/SilentModeModule';
export * from './src/SilentMode.types';
