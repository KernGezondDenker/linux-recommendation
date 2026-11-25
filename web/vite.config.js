import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite n'est pas un framework : c'est l'OUTIL DE CONSTRUCTION.
// Un navigateur ne comprend ni le JSX de React (<Bouton />) ni les
// modules ES importes entre fichiers. Vite convertit tout cela en
// HTML/CSS/JavaScript ordinaire, et fournit en developpement un serveur
// qui recharge la page a chaque sauvegarde.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Equivalent du relais nginx, pour "npm run dev" hors Docker :
    // le serveur de developpement transmet /api/... et /actuator/...
    // au service lance sur le poste. Sans cela, l'appel relatif du
    // front viserait le serveur de Vite, qui ne connait pas l'API.
    proxy: {
      '/api': 'http://localhost:8082',
      '/actuator': 'http://localhost:8082',
    },
  },
})
