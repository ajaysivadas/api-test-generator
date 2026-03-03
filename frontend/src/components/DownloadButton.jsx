import React, { useState } from 'react'
import { downloadFramework } from '../services/api'

function DownloadButton({ generationId, disabled }) {
  const [downloading, setDownloading] = useState(false)

  const handleDownload = async () => {
    if (!generationId) return
    setDownloading(true)
    try {
      await downloadFramework(generationId)
    } catch (error) {
      console.error('Download failed:', error)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <button
      onClick={handleDownload}
      disabled={disabled || !generationId || downloading}
      className={`inline-flex items-center gap-2 px-6 py-3 rounded-lg text-sm font-medium transition-all ${
        disabled || !generationId
          ? 'bg-gray-800 text-gray-600 cursor-not-allowed'
          : 'bg-indigo-600 text-white hover:bg-indigo-500 shadow-sm hover:shadow'
      }`}
    >
      {downloading ? (
        <>
          <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          Downloading...
        </>
      ) : (
        <>
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Download Framework
        </>
      )}
    </button>
  )
}

export default DownloadButton
