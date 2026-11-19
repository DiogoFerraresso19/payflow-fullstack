import { useState } from "react";
import { Wallet, ArrowRight, UserPlus, LogIn } from "lucide-react";
import api from "../services/api";

export default function AuthPage({ onLoginSuccess }) {
  // Alterna entre modo Login (true) e modo Cadastro (false)
  const [isLogin, setIsLogin] = useState(true);

  // Estados dos campos do formulário
  const [nome, setNome] = useState("");
  const [cpf, setCpf] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");

  // Estados de feedback visual (erro e carregamento)
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault(); // Evita que a página recarregue ao enviar o formulário
    setErro("");
    setCarregando(true);

    try {
      // Escolhe o endpoint e o corpo da requisição conforme a aba ativa
      const endpoint = isLogin ? "/auth/login" : "/auth/register";
      const payload = isLogin
        ? { email, senha }
        : { nome, cpf: cpf.replace(/\D/g, ""), email, senha }; // Remove pontos/traços do CPF

      const response = await api.post(endpoint, payload);

      // Salva o Token JWT no armazenamento local do navegador
      localStorage.setItem("payflow_token", response.data.token);

      // Avisa o App.jsx que o usuário autenticou para trocar para o Dashboard
      onLoginSuccess();
    } catch (err) {
      // Lê a mensagem amigável devolvida pelo nosso GlobalExceptionHandler no Spring Boot
      setErro(err.response?.data?.erro || "Erro ao conectar com o servidor.");
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="logo">
          <Wallet size={32} color="#10b981" />
          <h1>PayFlow</h1>
        </div>
        <p className="subtitle">
          {isLogin
            ? "Acesse sua carteira digital"
            : "Abra sua conta e ganhe R$ 1.000,00 de bônus"}
        </p>

        {erro && <div className="alert-error">{erro}</div>}

        <form onSubmit={handleSubmit}>
          {!isLogin && (
            <>
              <input
                type="text"
                placeholder="Nome completo"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                required
              />
              <input
                type="text"
                placeholder="CPF (apenas 11 números)"
                maxLength={11}
                value={cpf}
                onChange={(e) => setCpf(e.target.value)}
                required
              />
            </>
          )}

          <input
            type="email"
            placeholder="Seu e-mail"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="Sua senha (mínimo 6 caracteres)"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            required
          />

          <button type="submit" className="btn-primary" disabled={carregando}>
            {carregando ? (
              "Processando..."
            ) : isLogin ? (
              <>
                <LogIn size={18} /> Entrar na Conta
              </>
            ) : (
              <>
                <UserPlus size={18} /> Criar Conta Grátis
              </>
            )}
          </button>
        </form>

        <button
          type="button"
          className="btn-link"
          onClick={() => {
            setIsLogin(!isLogin);
            setErro("");
          }}
        >
          {isLogin
            ? "Não tem conta? Cadastre-se agora"
            : "Já tem conta? Faça login"}
          <ArrowRight size={14} />
        </button>
      </div>
    </div>
  );
}
