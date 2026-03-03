import React, { useState, useEffect } from 'react'
import DownloadButton from '../components/DownloadButton'
import { getHistory } from '../services/api'

function History() {
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    loadHistory()
  }, [])

  const loadHistory = async () => {
    try {
      const data = await getHistory()
      setHistory(data)
    } catch (err) {
      setError(err.response?.data?.error || err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-100">Generation History</h2>
          <p className="text-gray-500 mt-1">View and download previously generated frameworks</p>
        </div>
        <button
          onClick={loadHistory}
          className="px-4 py-2 text-sm text-gray-400 hover:text-gray-200 border border-gray-700 rounded-lg hover:bg-gray-800 transition-colors"
        >
          Refresh
        </button>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-12">
          <div className="flex items-center gap-3 text-indigo-400">
            <svg className="w-6 h-6 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            <span className="font-medium">Loading history...</span>
          </div>
        </div>
      )}

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4">
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}

      {!loading && history.length === 0 && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-12 text-center">
          <svg className="w-12 h-12 text-gray-700 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-gray-400">No generation history yet</p>
          <p className="text-sm text-gray-600 mt-1">Generate your first framework to see it here</p>
        </div>
      )}

      <div className="space-y-4">
        {history.map((entry) => (
          <div key={entry.generationId} className="bg-gray-900 rounded-xl border border-gray-800 p-6">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-lg font-semibold text-gray-100">{entry.specTitle}</h3>
                <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                  <span>{entry.sourceFormat}</span>
                  <span>&middot;</span>
                  <span>{entry.basePackage || entry.serviceName}</span>
                  <span>&middot;</span>
                  <span>{new Date(entry.timestamp).toLocaleString()}</span>
                  {entry.mode === 'merge' && (
                    <>
                      <span>&middot;</span>
                      <span className="text-xs px-1.5 py-0.5 rounded bg-purple-500/15 text-purple-400 font-medium">Merge</span>
                    </>
                  )}
                </div>
              </div>
              <DownloadButton generationId={entry.generationId} />
            </div>
            <div className="grid grid-cols-4 gap-4 mt-4">
              <Stat label="Endpoints" value={entry.endpointCount} />
              <Stat label="Schemas" value={entry.schemaCount} />
              <Stat label="POJOs" value={entry.pojoCount} />
              <Stat label="Test Classes" value={entry.testCount} />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function Stat({ label, value }) {
  return (
    <div className="bg-gray-800/50 rounded-lg p-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-lg font-bold text-gray-100">{value}</p>
    </div>
  )
}

export default History
