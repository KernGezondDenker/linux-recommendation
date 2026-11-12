import { useState } from 'react'

// l'url de l'api vient d'une variable lue par vite AU MOMENT DU BUILD.
// jamais ecrite en dur : elle differe entre le poste de dev et le serveur
const API = import.meta.env.VITE_RECOMMENDATION_URL || 'http://localhost:8082'

// les quatre champs numeriques, decrits une fois au lieu d'etre recopies
// quatre fois. 'min' et 'max' reprennent EXACTEMENT les @Min/@Max du
// record Request cote java : si les deux listes divergent, le navigateur
// laisse passer une valeur que le serveur refusera par un 400
const CHAMPS = 
[
  { nom: 'ramMb',     libelle: 'Memoire vive (Mo)',            min: 128, max: 1048576 },
  { nom: 'storageGb', libelle: 'Stockage disponible (Go)',     min: 1,   max: 1048576 },
  { nom: 'cpuMhz',    libelle: 'Frequence du processeur (MHz)', min: 1,  max: 100000 },
  { nom: 'cpuCores',  libelle: 'Nombre de coeurs',             min: 1,   max: 256 },
]

// les CODES correspondent a use_case.code en base et ne changent jamais.
// les libelles sont affiches et peuvent etre reformules sans rien casser :
// c'est tout l'interet d'avoir separe les deux colonnes
const USAGES = 
[
  { code: 'OFFICE',      libelle: 'Bureautique et navigation' },
  { code: 'GAMING',      libelle: 'Jeux video' },
  { code: 'CREATIVE',    libelle: 'Creation multimedia (image, photo, video)' },
  { code: 'DEVELOPMENT', libelle: 'Developpement et programmation' },
  { code: 'EDUCATION',   libelle: 'Usage scolaire et educatif' },
]

// les cinq descripteurs sont repris mot pour mot de la grille de codage.
// le moteur calcule |difficulty_level - niveau_declare| : la soustraction
// n'aurait aucun sens si les deux echelles ne decrivaient pas la meme chose
const NIVEAUX = [
  "Je n'ai jamais rien installé par moi-même",
  'Je sais installer un programme et modifier des réglages',
  "Je suis à l'aise, j'ai déjà tapé des commandes",
  'Je configure mon systeme par moi-même',
  'Je sais assembler et administrer un systeme Linux',
]

export default function App() {

  // UN seul objet d'etat pour tout le formulaire. ses cles portent les
  // noms EXACTS attendus par le record Request : on peut donc l'envoyer
  // tel quel, sans etape de traduction
  const [form, setForm] = useState({
    ramMb: 8192,
    storageGb: 256,
    cpuMhz: 2400,
    cpuCores: 4,
    is32bitOnly: false,
    hasProprietaryGpu: true,
    skillLevel: 2,
    selectedUseCases: ['OFFICE', 'CREATIVE'],
  })

  const [resultats, setResultats] = useState(null)
  const [erreur, setErreur] = useState(null)
  const [chargement, setChargement] = useState(false)

  // on ne modifie JAMAIS l'objet existant : on en construit un nouveau.
  // react compare les references pour savoir s'il doit reafficher, donc
  // une modification sur place passerait inapercue
  const majChamp = (nom, valeur) => setForm(p => ({ ...p, [nom]: valeur }))

  const basculerUsage = code => setForm(p => ({
    ...p,
    selectedUseCases: p.selectedUseCases.includes(code)
      ? p.selectedUseCases.filter(c => c !== code)   // deja coche -> on retire
      : [...p.selectedUseCases, code],               // sinon -> on ajoute
  }))

  async function envoyer(e) {
    e.preventDefault()          // sinon le navigateur recharge la page
    setChargement(true)
    setErreur(null)
    setResultats(null)

    try {
      const r = await fetch(`${API}/api/v1/recommendations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      })

      // trois cas distincts, trois messages distincts. les confondre
      // reviendrait a dire "ca ne marche pas" sans dire ou chercher
      if (r.status === 400) throw new Error('Une valeur saisie est hors bornes.')
      if (!r.ok) throw new Error(`Le service a repondu ${r.status}.`)

      // la reponse est un objet { fallbackApplied, results }. results compte
      // 1 a 3 entrees, jamais vide : la regle RG-01 le garantit cote serveur
      setResultats(await r.json())

    } catch (err) {
      // on arrive aussi ici si le reseau tombe ou si CORS bloque
      setErreur(err.message)
    } finally {
      setChargement(false)      // execute dans les deux cas
    }
  }

  return (
    <main>
      <h1>Quelle distribution Linux pour votre machine&nbsp;?</h1>
      <p className="sous-titre">Maquette technique. Le catalogue compte cinq distributions.</p>

      {resultats && (
        <section aria-live="polite">
          <h2>Resultats</h2>
          {/* RG-01 : si le moteur a utilise le repli, on previent l'utilisateur
          qu'aucune distribution ne convient vraiment a sa machine */}
          {resultats.fallbackApplied && (
            <p role="alert">
              Aucune distribution du catalogue ne satisfait toutes les exigences
              de votre machine. Voici les plus légères, à titre indicatif.
            </p>
          )}
          {resultats.results.map(d => (
            <article key={d.distributionId}>
              <header>
                <h3>{d.name}</h3>
                <span className="score">{d.score}</span>
              </header>
              {/* RG-03 : on affiche les motifs produits par le moteur,
                  jamais un texte redige ici. c'est ce qui garantit que
                  l'explication montree est bien celle du calcul */}
              <ul>
                {d.reasons.map((motif, i) => <li key={i}>{motif}</li>)}
              </ul>
            </article>
          ))}
        </section>
      )}

      <form onSubmit={envoyer}>
      <fieldset>
          <legend>Votre profil</legend>

          <label>
            <span>Votre niveau</span>
            <select value={form.skillLevel}onChange={e => majChamp('skillLevel', Number(e.target.value))}>{NIVEAUX.map((libelle, i) => (<option key={i} value={i + 1}>{i + 1} — {libelle}</option>))}
            </select>
          </label>

          <div className="cases">
            <span>Vos usages</span>
            {USAGES.map(u => (
              <label key={u.code}>
                <input type="checkbox"
                       checked={form.selectedUseCases.includes(u.code)}
                       onChange={() => basculerUsage(u.code)} />
                <span>{u.libelle}</span>
              </label>
            ))}
          </div>
        </fieldset>

      
        <fieldset>
          <legend>Votre materiel</legend>

          {CHAMPS.map(c => (
            <label key={c.nom}>
              <span>{c.libelle}</span>
              <input type="number" required min={c.min} max={c.max}
                     value={form[c.nom]}
                     onChange={e => majChamp(c.nom, Number(e.target.value))} />
            </label>
          ))}

          <div className="cases">
            <label>
              <input type="checkbox" checked={form.hasProprietaryGpu}
                     onChange={e => majChamp('hasProprietaryGpu', e.target.checked)} />
              <span>Carte graphique NVIDIA</span>
            </label>
            <label>
              <input type="checkbox" checked={form.is32bitOnly}
                     onChange={e => majChamp('is32bitOnly', e.target.checked)} />
              <span>Processeur 32 bits uniquement</span>
            </label>
          </div>
        </fieldset>

        

        <button type="submit" disabled={chargement}>
          {chargement ? 'Calcul en cours…' : 'Obtenir une recommandation'}
        </button>
      </form>

      {erreur && <p className="erreur" role="alert">{erreur}</p>}

      
    </main>
  )
}
