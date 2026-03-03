import React, { useState } from 'react'

function EnumEntriesPreview({ endpointEntries, baseUriEntry }) {
  const [copied, setCopied] = useState(null)

  const copyToClipboard = (text, key) => {
    navigator.clipboard.writeText(text)
    setCopied(key)
    setTimeout(() => setCopied(null), 2000)
  }

  const endpointEntriesText = endpointEntries.map(e => `    ${e},`).join('\n')
  const baseUriText = `    ${baseUriEntry},`

  return (
    <div className="space-y-4">
      <div>
        <h4 className="text-sm font-semibold text-gray-100">One Manual Step Required</h4>
        <p className="text-xs text-gray-500 mt-1">
          The generated code references these enum constants. Copy and paste them into your existing enum files so the new code compiles.
        </p>
      </div>

      {/* BaseUri Entry */}
      <div className="bg-amber-500/5 border border-amber-500/20 rounded-lg p-4">
        <div className="flex items-center justify-between mb-1">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-amber-400 bg-amber-500/15 px-2 py-0.5 rounded">STEP 1</span>
            <span className="text-sm font-medium text-gray-200">Add to BaseUri.java</span>
          </div>
          <button
            onClick={() => copyToClipboard(baseUriText, 'baseUri')}
            className="text-xs px-3 py-1 bg-amber-500/15 text-amber-400 rounded-md hover:bg-amber-500/25 transition-colors font-medium"
          >
            {copied === 'baseUri' ? 'Copied!' : 'Copy'}
          </button>
        </div>
        <p className="text-xs text-gray-500 mb-2">
          Open <code className="bg-gray-800 px-1 rounded text-gray-400">Base/URI/BaseUri.java</code> and add this entry to the enum:
        </p>
        <pre className="text-sm text-gray-300 font-mono bg-gray-900 rounded p-3 border border-gray-800 overflow-x-auto">
{baseUriText}
        </pre>
      </div>

      {/* EndPoint Entries */}
      <div className="bg-amber-500/5 border border-amber-500/20 rounded-lg p-4">
        <div className="flex items-center justify-between mb-1">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-amber-400 bg-amber-500/15 px-2 py-0.5 rounded">STEP 2</span>
            <span className="text-sm font-medium text-gray-200">Add to EndPoint.java</span>
          </div>
          <button
            onClick={() => copyToClipboard(endpointEntriesText, 'endpoints')}
            className="text-xs px-3 py-1 bg-amber-500/15 text-amber-400 rounded-md hover:bg-amber-500/25 transition-colors font-medium"
          >
            {copied === 'endpoints' ? 'Copied!' : 'Copy All'}
          </button>
        </div>
        <p className="text-xs text-gray-500 mb-2">
          Open <code className="bg-gray-800 px-1 rounded text-gray-400">Base/URI/EndPoint.java</code> and add these {endpointEntries.length} entries to the enum:
        </p>
        <pre className="text-sm text-gray-300 font-mono bg-gray-900 rounded p-3 border border-gray-800 overflow-x-auto max-h-48 overflow-y-auto">
{endpointEntriesText}
        </pre>
      </div>
    </div>
  )
}

export default EnumEntriesPreview
