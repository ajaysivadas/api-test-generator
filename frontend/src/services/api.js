import axios from 'axios'

const API_BASE = '/api'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 60000,
})

export const parseSpec = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await api.post('/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export const generateFramework = async (file, basePackage = 'com.example.api') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('basePackage', basePackage)
  const response = await api.post('/generate', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export const mergeIntoFramework = async (file, serviceName, baseUriKey, baseUriEnumName) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('serviceName', serviceName)
  formData.append('baseUriKey', baseUriKey)
  if (baseUriEnumName) {
    formData.append('baseUriEnumName', baseUriEnumName)
  }
  const response = await api.post('/merge', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export const downloadMerge = async (generationId, serviceName) => {
  const response = await api.get(`/download/${generationId}`, {
    responseType: 'blob',
  })
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = url
  const filename = serviceName ? `${serviceName.toLowerCase()}-merge.zip` : 'merge.zip'
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

export const compareSpecs = async (oldFile, newFile) => {
  const formData = new FormData()
  formData.append('oldFile', oldFile)
  formData.append('newFile', newFile)
  const response = await api.post('/compare', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export const downloadFramework = async (generationId) => {
  const response = await api.get(`/download/${generationId}`, {
    responseType: 'blob',
  })
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', 'generated-framework.zip')
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

export const getHistory = async () => {
  const response = await api.get('/history')
  return response.data
}

export default api
