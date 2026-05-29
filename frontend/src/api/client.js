import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

export async function ensureCsrf() {
  await api.get('/auth/csrf')
}

function readCookie(name) {
  return document.cookie
    .split(';')
    .map(item => item.trim())
    .find(item => item.startsWith(`${name}=`))
    ?.slice(name.length + 1)
}

function isMutating(method) {
  return !['get', 'head', 'options'].includes((method || 'get').toLowerCase())
}

api.interceptors.request.use(config => {
  if (isMutating(config.method)) {
    const token = readCookie('XSRF-TOKEN')
    if (token) {
      config.headers['X-XSRF-TOKEN'] = decodeURIComponent(token)
    }
  }
  return config
})

api.interceptors.response.use(
  response => response,
  async error => {
    const config = error.config || {}
    if (error.response?.status === 403 && isMutating(config.method) && !config._csrfRetry) {
      config._csrfRetry = true
      await ensureCsrf()
      return api(config)
    }
    const message = error.response?.data?.message || error.response?.data?.error || error.message
    return Promise.reject(new Error(message))
  }
)
