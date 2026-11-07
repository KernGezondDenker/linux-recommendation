import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite n'est pas un framework : c'est l'OUTIL DE CONSTRUCTION.
// Un navigateur ne comprend ni le JSX de React (<Bouton />) ni les
// modules ES importes entre fichiers. Vite convertit tout cela en
// HTML/CSS/JavaScript ordinaire, et fournit en developpement un serveur
// qui recharge la page a chaque sauvegarde.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 }
})
