// GitHub Pages SPA fallback: copy index.html to 404.html
// Vue Router history mode needs this to handle direct URL access
import { copyFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const dist = resolve(__dirname, '..', 'dist')

copyFileSync(resolve(dist, 'index.html'), resolve(dist, '404.html'))
console.log('✓ Created dist/404.html for SPA routing fallback')
