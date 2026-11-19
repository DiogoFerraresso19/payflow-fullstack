import axios from "axios";

// Cria uma instância do Axios apontando para o nosso Backend Java
const api = axios.create({
  baseURL: "http://localhost:8080/api",
});

// Interceptor de Requisição: roda AUTOMATICAMENTE antes de qualquer chamada sair do React
api.interceptors.request.use((config) => {
  // Busca o token salvo no navegador após o login
  const token = localStorage.getItem("payflow_token");

  // Se existir token, injeta no cabeçalho Authorization (exatamente como o JwtAuthenticationFilter espera)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

export default api;
