import viteLogo from '/vite.svg'
import './App.css'
import PatternUpload from './components/PatternUpload'

function App() {
  return (
    <>
      <div>
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
      </div>
      <h1>Vite + React</h1>
      <PatternUpload />
    </>
  )
}

export default App
