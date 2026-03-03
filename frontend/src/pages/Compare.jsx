import React, { useState } from 'react'
import FileUpload from '../components/FileUpload'
import SchemaChanges from '../components/SchemaChanges'
import { compareSpecs } from '../services/api'

function Compare() {
  const [oldFile, setOldFile] = useState(null)
  const [newFile, setNewFile] = useState(null)
  const [comparison, setComparison] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleCompare = async () => {
    if (!oldFile || !newFile) return

    setLoading(true)
    setError(null)
    setComparison(null)

    try {
      const result = await compareSpecs(oldFile, newFile)
      if (result.success) {
        setComparison(result)
      } else {
        setError(result.error)
      }
    } catch (err) {
      setError(err.response?.data?.error || err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-gray-100">Compare API Specs</h2>
        <p className="text-gray-500 mt-1">Detect breaking changes and schema differences between two API versions</p>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div>
          <h3 className="text-sm font-medium text-gray-300 mb-3">Old Version</h3>
          <FileUpload
            onFileSelect={setOldFile}
            label="Upload Old API Spec"
          />
          {oldFile && (
            <p className="text-sm text-green-400 mt-2">Selected: {oldFile.name}</p>
          )}
        </div>
        <div>
          <h3 className="text-sm font-medium text-gray-300 mb-3">New Version</h3>
          <FileUpload
            onFileSelect={setNewFile}
            label="Upload New API Spec"
          />
          {newFile && (
            <p className="text-sm text-green-400 mt-2">Selected: {newFile.name}</p>
          )}
        </div>
      </div>

      <button
        onClick={handleCompare}
        disabled={!oldFile || !newFile || loading}
        className="px-6 py-3 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {loading ? (
          <span className="flex items-center gap-2">
            <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            Comparing...
          </span>
        ) : (
          'Compare Specs'
        )}
      </button>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4">
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}

      {comparison && (
        <>
          <div className="bg-gray-900 rounded-xl border border-gray-800 p-4">
            <div className="flex items-center gap-4 text-sm">
              <span className="text-gray-500">Comparing:</span>
              <span className="font-medium text-gray-200">{comparison.oldSpecTitle}</span>
              <svg className="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
              <span className="font-medium text-gray-200">{comparison.newSpecTitle}</span>
            </div>
          </div>
          <SchemaChanges comparison={comparison} />
        </>
      )}
    </div>
  )
}

export default Compare
