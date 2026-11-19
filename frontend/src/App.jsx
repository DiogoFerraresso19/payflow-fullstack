import { useState } from "react";
import AuthPage from "./pages/AuthPage";
import DashboardPage from "./pages/DashboardPage";
import "./App.css";

export default function App() {
  // Verifica se já existe um token salvo no navegador ao abrir a página
  const [autenticado, setAutenticado] = useState(
    !!localStorage.getItem("payflow_token"),
  );

  function handleLogout() {
    localStorage.removeItem("payflow_token");
    document.title = "Conta PayFlow";
    setAutenticado(false);
  }

  return autenticado ? (
    <DashboardPage onLogout={handleLogout} />
  ) : (
    <AuthPage onLoginSuccess={() => setAutenticado(true)} />
  );
}
