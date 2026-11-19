import { useEffect, useState } from "react";
import {
  Wallet,
  Send,
  Eye,
  EyeOff,
  LogOut,
  Copy,
  Check,
  ArrowUpRight,
  ArrowDownLeft,
  RotateCcw,
} from "lucide-react";
import api from "../services/api";

export default function DashboardPage({ onLogout }) {
  const [dados, setDados] = useState(null);
  const [mostrarSaldo, setMostrarSaldo] = useState(true);
  const [copiado, setCopiado] = useState(false);

  // Estados do formulário de transferência Pix
  const [chavePixDestino, setChavePixDestino] = useState("");
  const [valor, setValor] = useState("");
  const [mensagem, setMensagem] = useState({ tipo: "", texto: "" });
  const [enviando, setEnviando] = useState(false);

  // Formata números para o padrão brasileiro (ex: R$ 1.000,00)
  const formatarMoeda = (val) =>
    Number(val).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

  // Carrega saldo, chaves e extrato do backend
  async function carregarDashboard() {
    try {
      const response = await api.get("/pix/dashboard");
      setDados(response.data);
    } catch (err) {
      // Se o token expirar (erro 401/403), desloga automaticamente
      if (err.response?.status === 401 || err.response?.status === 403) {
        onLogout();
      }
    }
  }

  // Executa automaticamente assim que a tela abre
  useEffect(() => {
    carregarDashboard();
  }, []);

  // Atualiza o título da aba do navegador com o nome do usuário logado
  useEffect(() => {
    if (dados?.nomeUsuario) {
      document.title = `Conta PayFlow | ${dados.nomeUsuario}`;
    } else {
      document.title = "Conta PayFlow";
    }
  }, [dados]);

  // Envia transferência Pix com Idempotency-Key
  async function handleTransferir(e) {
    e.preventDefault();
    setMensagem({ tipo: "", texto: "" });
    setEnviando(true);

    try {
      // Gera um UUID único para esta intenção de transferência (evita cobrança dupla)
      const idempotencyKey = crypto.randomUUID();

      await api.post(
        "/pix/transferir",
        { chavePixDestino, valor: parseFloat(valor) },
        { headers: { "Idempotency-Key": idempotencyKey } },
      );

      setMensagem({
        tipo: "sucesso",
        texto: "Transferência Pix realizada com sucesso!",
      });
      setChavePixDestino("");
      setValor("");
      carregarDashboard(); // Atualiza saldo e extrato na hora
    } catch (err) {
      setMensagem({
        tipo: "erro",
        texto: err.response?.data?.erro || "Falha ao realizar transferência.",
      });
    } finally {
      setEnviando(false);
    }
  }

  // Estorna uma transferência recebida
  async function handleEstornar(transacaoId) {
    if (!window.confirm("Deseja realmente devolver este Pix para o remetente?"))
      return;

    try {
      await api.post(`/pix/estornar/${transacaoId}`);
      setMensagem({ tipo: "sucesso", texto: "Estorno realizado com sucesso!" });
      carregarDashboard();
    } catch (err) {
      setMensagem({
        tipo: "erro",
        texto: err.response?.data?.erro || "Não foi possível estornar.",
      });
    }
  }

  function copiarChave(chave) {
    navigator.clipboard.writeText(chave);
    setCopiado(true);
    setTimeout(() => setCopiado(false), 2000);
  }

  if (!dados)
    return <div className="loading">Carregando sua conta PayFlow...</div>;

  return (
    <div className="dashboard-container">
      {/* Cabeçalho */}
      <header className="navbar">
        <div className="logo">
          <Wallet size={28} color="#10b981" />
          <span>PayFlow</span>
        </div>
        <div className="user-info">
          <span>
            Olá, <strong>{dados.nomeUsuario}</strong> (Conta:{" "}
            {dados.numeroConta})
          </span>
          <button onClick={onLogout} className="btn-logout" title="Sair">
            <LogOut size={18} /> Sair
          </button>
        </div>
      </header>

      <main className="main-grid">
        {/* Coluna Esquerda: Saldo + Transferência Pix */}
        <section className="left-column">
          <div className="card balance-card">
            <div className="card-header">
              <span>Saldo disponível</span>
              <button
                onClick={() => setMostrarSaldo(!mostrarSaldo)}
                className="icon-btn"
              >
                {mostrarSaldo ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
            <h2 className="balance-value">
              {mostrarSaldo ? formatarMoeda(dados.saldo) : "R$ ••••••"}
            </h2>

            {dados.chavesPix.length > 0 && (
              <div className="pix-key-box">
                <span>
                  Sua chave Pix (CPF): <strong>{dados.chavesPix[0]}</strong>
                </span>
                <button
                  onClick={() => copiarChave(dados.chavesPix[0])}
                  className="icon-btn"
                >
                  {copiado ? (
                    <Check size={16} color="#10b981" />
                  ) : (
                    <Copy size={16} />
                  )}
                </button>
              </div>
            )}
          </div>

          <div className="card">
            <h3>
              <Send size={18} color="#10b981" /> Transferir via Pix
            </h3>

            {mensagem.texto && (
              <div
                className={
                  mensagem.tipo === "sucesso" ? "alert-success" : "alert-error"
                }
              >
                {mensagem.texto}
              </div>
            )}

            <form onSubmit={handleTransferir}>
              <label>Chave Pix de destino (CPF)</label>
              <input
                type="text"
                placeholder="Ex: 98765432100"
                value={chavePixDestino}
                onChange={(e) => setChavePixDestino(e.target.value)}
                required
              />

              <label>Valor (R$)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0,00"
                value={valor}
                onChange={(e) => setValor(e.target.value)}
                required
              />

              <button type="submit" className="btn-primary" disabled={enviando}>
                {enviando ? "Enviando Pix..." : "Confirmar Transferência"}
              </button>
            </form>
          </div>
        </section>

        {/* Coluna Direita: Extrato (Ledger) */}
        <section className="card history-card">
          <h3>Histórico de Movimentações</h3>

          {dados.extrato.length === 0 ? (
            <p className="empty-text">Nenhuma movimentação encontrada.</p>
          ) : (
            <ul className="transaction-list">
              {dados.extrato.map((item) => {
                const isEntrada = item.direcao === "ENTRADA";
                return (
                  <li key={item.id} className="transaction-item">
                    <div className="tx-left">
                      <div className={`tx-icon ${isEntrada ? "in" : "out"}`}>
                        {isEntrada ? (
                          <ArrowDownLeft size={18} />
                        ) : (
                          <ArrowUpRight size={18} />
                        )}
                      </div>
                      <div>
                        <strong>{item.nomeContraparte}</strong>
                        <small>
                          {item.tipo} •{" "}
                          {new Date(item.dataHora).toLocaleString("pt-BR")}
                        </small>
                      </div>
                    </div>

                    <div className="tx-right">
                      <span className={isEntrada ? "amount-in" : "amount-out"}>
                        {isEntrada ? "+" : "-"} {formatarMoeda(item.valor)}
                      </span>

                      {/* Botão de Estorno aparece apenas em transferências recebidas que ainda não foram estornadas */}
                      {isEntrada &&
                        item.tipo === "TRANSFERENCIA" &&
                        !item.jaEstornada && (
                          <button
                            onClick={() => handleEstornar(item.id)}
                            className="btn-refund"
                            title="Devolver este Pix"
                          >
                            <RotateCcw size={14} /> Estornar
                          </button>
                        )}
                      {item.jaEstornada && (
                        <span className="badge-refunded">Estornada</span>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
