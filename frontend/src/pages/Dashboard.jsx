import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import FileUpload from '../components/FileUpload'
import CodePreview from '../components/CodePreview'
import { parseSpec, getHistory, downloadFramework, downloadMerge } from '../services/api'

function Dashboard() {
  const [parseResult, setParseResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [history, setHistory] = useState([])
  const [historyLoading, setHistoryLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    loadHistory()
  }, [])

  const loadHistory = async () => {
    try {
      const data = await getHistory()
      setHistory(data)
    } catch {
      // silently ignore
    } finally {
      setHistoryLoading(false)
    }
  }

  const handleFileSelect = async (file) => {
    setLoading(true)
    setError(null)
    try {
      const result = await parseSpec(file)
      if (result.success) {
        setParseResult(result)
      } else {
        setError(result.error)
      }
    } catch (err) {
      setError(err.response?.data?.error || err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleDownload = (entry) => {
    if (entry.mode === 'merge') {
      downloadMerge(entry.generationId, entry.serviceName)
    } else {
      downloadFramework(entry.generationId)
    }
  }

  const totalGenerated = history.length
  const totalTests = history.reduce((sum, e) => sum + (e.testCount || e.testClassCount || 0), 0)
  const totalEndpoints = history.reduce((sum, e) => sum + (e.endpointCount || e.executorMethodCount || 0), 0)

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Hero Section */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-600 via-indigo-700 to-purple-800 p-8">
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/3" />
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-white/5 rounded-full translate-y-1/2 -translate-x-1/4" />
        <div className="relative">
          <h2 className="text-3xl font-bold text-white">API Automation Agent</h2>
          <p className="text-indigo-200 mt-2 max-w-lg">
            Generate production-ready REST Assured + TestNG test frameworks from your API specifications in seconds.
          </p>
          <div className="flex gap-3 mt-6">
            <button
              onClick={() => navigate('/generate')}
              className="px-5 py-2.5 bg-white text-indigo-700 rounded-lg text-sm font-semibold hover:bg-indigo-50 transition-colors shadow-lg shadow-indigo-900/30"
            >
              Get Started
            </button>
            <button
              onClick={() => navigate('/compare')}
              className="px-5 py-2.5 bg-white/10 text-white rounded-lg text-sm font-medium hover:bg-white/20 transition-colors backdrop-blur-sm border border-white/20"
            >
              Compare Specs
            </button>
          </div>
        </div>
      </div>

      {/* Bento Grid - Actions + Stats */}
      <div className="grid grid-cols-12 gap-4">
        {/* Generate - Primary Action (large card) */}
        <button
          onClick={() => navigate('/generate')}
          className="col-span-12 sm:col-span-7 bg-gray-900 rounded-2xl border border-gray-800 p-6 text-left hover:border-indigo-500/30 transition-all group relative overflow-hidden"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
          <div className="relative flex items-start justify-between">
            <div>
              <div className="w-12 h-12 rounded-xl bg-indigo-500/15 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                <svg className="w-6 h-6 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                </svg>
              </div>
              <h3 className="text-lg font-semibold text-gray-100">Generate Framework</h3>
              <p className="text-sm text-gray-500 mt-1 max-w-sm">
                Upload an OpenAPI or Postman spec and generate a complete test automation framework, or merge into your existing one.
              </p>
              <div className="flex items-center gap-4 mt-4">
                <span className="text-xs px-2.5 py-1 rounded-full bg-indigo-500/10 text-indigo-400 font-medium">Standalone</span>
                <span className="text-xs px-2.5 py-1 rounded-full bg-purple-500/10 text-purple-400 font-medium">Merge Mode</span>
                <span className="text-xs px-2.5 py-1 rounded-full bg-green-500/10 text-green-400 font-medium">OpenAPI + Postman</span>
              </div>
            </div>
            <svg className="w-5 h-5 text-gray-700 group-hover:text-indigo-400 group-hover:translate-x-1 transition-all flex-shrink-0 mt-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </div>
        </button>

        {/* Right column - stacked cards */}
        <div className="col-span-12 sm:col-span-5 grid grid-rows-2 gap-4">
          <button
            onClick={() => navigate('/compare')}
            className="bg-gray-900 rounded-2xl border border-gray-800 p-5 text-left hover:border-green-500/30 transition-all group relative overflow-hidden"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-green-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
            <div className="relative flex items-start justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-green-500/15 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
                <p className="text-sm font-semibold text-gray-100">Compare Specs</p>
                <p className="text-xs text-gray-500 mt-1">Detect breaking changes between versions</p>
              </div>
              <svg className="w-4 h-4 text-gray-700 group-hover:text-green-400 group-hover:translate-x-1 transition-all flex-shrink-0 mt-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>

          <button
            onClick={() => navigate('/history')}
            className="bg-gray-900 rounded-2xl border border-gray-800 p-5 text-left hover:border-amber-500/30 transition-all group relative overflow-hidden"
          >
            <div className="absolute inset-0 bg-gradient-to-br from-amber-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
            <div className="relative flex items-start justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-amber-500/15 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <svg className="w-5 h-5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <p className="text-sm font-semibold text-gray-100">History</p>
                <p className="text-xs text-gray-500 mt-1">Re-download previous frameworks</p>
              </div>
              <svg className="w-4 h-4 text-gray-700 group-hover:text-amber-400 group-hover:translate-x-1 transition-all flex-shrink-0 mt-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </button>
        </div>
      </div>

      {/* Stats Strip */}
      {totalGenerated > 0 && (
        <div className="grid grid-cols-3 gap-4">
          <StatCard value={totalGenerated} label="Frameworks Generated" accent="indigo" />
          <StatCard value={totalTests} label="Test Classes Created" accent="green" />
          <StatCard value={totalEndpoints} label="Endpoints Covered" accent="amber" />
        </div>
      )}

      {/* Recent Activity */}
      {!historyLoading && history.length > 0 && (
        <div className="bg-gray-900 rounded-2xl border border-gray-800 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              <h3 className="text-sm font-semibold text-gray-100">Recent Activity</h3>
            </div>
            <button
              onClick={() => navigate('/history')}
              className="text-xs text-gray-500 hover:text-indigo-400 font-medium transition-colors"
            >
              View All
            </button>
          </div>
          <div>
            {history.slice(0, 4).map((entry, index) => (
              <div
                key={entry.generationId}
                className={`px-6 py-4 flex items-center justify-between hover:bg-gray-800/30 transition-colors ${
                  index !== Math.min(3, history.length - 1) ? 'border-b border-gray-800/50' : ''
                }`}
              >
                <div className="flex items-center gap-4 min-w-0">
                  <div className="relative">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      entry.mode === 'merge'
                        ? 'bg-gradient-to-br from-purple-500/20 to-purple-500/5'
                        : 'bg-gradient-to-br from-indigo-500/20 to-indigo-500/5'
                    }`}>
                      {entry.mode === 'merge' ? (
                        <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                        </svg>
                      ) : (
                        <svg className="w-5 h-5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                        </svg>
                      )}
                    </div>
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-200 truncate">
                      {entry.specTitle || entry.serviceName || 'Untitled'}
                    </p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${
                        entry.mode === 'merge'
                          ? 'bg-purple-500/10 text-purple-400'
                          : 'bg-indigo-500/10 text-indigo-400'
                      }`}>
                        {entry.mode === 'merge' ? 'Merge' : 'Standalone'}
                      </span>
                      <span className="text-xs text-gray-600">
                        {formatRelativeTime(entry.timestamp)}
                      </span>
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => handleDownload(entry)}
                  className="text-xs px-3 py-1.5 bg-gray-800 text-gray-400 rounded-lg hover:bg-gray-700 hover:text-gray-200 transition-all font-medium flex-shrink-0 flex items-center gap-1.5"
                >
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                  </svg>
                  Download
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Quick Preview */}
      <div className="space-y-4">
        <div className="flex items-center gap-3">
          <div className="h-px flex-1 bg-gray-800" />
          <span className="text-xs font-medium text-gray-600 uppercase tracking-wider">Quick Preview</span>
          <div className="h-px flex-1 bg-gray-800" />
        </div>

        <FileUpload onFileSelect={handleFileSelect} label="Upload API Spec to Preview" />
      </div>

      {loading && (
        <div className="flex items-center justify-center py-12">
          <div className="flex items-center gap-3 text-indigo-400">
            <svg className="w-6 h-6 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            <span className="font-medium">Parsing API specification...</span>
          </div>
        </div>
      )}

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4">
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}

      {parseResult && (
        <>
          <div className="grid grid-cols-4 gap-4">
            <InfoCard label="API Title" value={parseResult.title} />
            <InfoCard label="Version" value={parseResult.version} />
            <InfoCard label="Endpoints" value={parseResult.endpointCount} />
            <InfoCard label="Schemas" value={parseResult.schemaCount} />
          </div>

          <div className="flex gap-3">
            <button
              onClick={() => navigate('/generate')}
              className="px-5 py-2.5 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-500 transition-colors"
            >
              Generate Framework
            </button>
            <button
              onClick={() => navigate('/compare')}
              className="px-5 py-2.5 bg-gray-800 border border-gray-700 text-gray-300 rounded-lg text-sm font-medium hover:bg-gray-700 transition-colors"
            >
              Compare Specs
            </button>
          </div>

          <CodePreview parseResult={parseResult} />
        </>
      )}
    </div>
  )
}

function StatCard({ value, label, accent }) {
  const colors = {
    indigo: { bar: 'bg-indigo-500', bg: 'bg-indigo-500/10', text: 'text-indigo-400' },
    green: { bar: 'bg-green-500', bg: 'bg-green-500/10', text: 'text-green-400' },
    amber: { bar: 'bg-amber-500', bg: 'bg-amber-500/10', text: 'text-amber-400' },
  }
  const c = colors[accent] || colors.indigo

  return (
    <div className="bg-gray-900 rounded-2xl border border-gray-800 p-5 relative overflow-hidden">
      <div className={`absolute top-0 left-0 right-0 h-0.5 ${c.bar}`} />
      <p className="text-3xl font-bold text-gray-100 tracking-tight">{value}</p>
      <p className="text-xs text-gray-500 mt-1">{label}</p>
    </div>
  )
}

function InfoCard({ label, value }) {
  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 p-4">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-xl font-bold text-gray-100 mt-1">{value || '-'}</p>
    </div>
  )
}

function formatRelativeTime(timestamp) {
  const now = new Date()
  const then = new Date(timestamp)
  const diffMs = now - then
  const diffMin = Math.floor(diffMs / 60000)
  const diffHr = Math.floor(diffMin / 60)
  const diffDays = Math.floor(diffHr / 24)

  if (diffMin < 1) return 'just now'
  if (diffMin < 60) return `${diffMin}m ago`
  if (diffHr < 24) return `${diffHr}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return then.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

export default Dashboard
