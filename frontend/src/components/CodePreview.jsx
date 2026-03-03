import React, { useState } from 'react'

function CodePreview({ parseResult }) {
  const [activeTab, setActiveTab] = useState('endpoints')

  if (!parseResult) return null

  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
      {/* Tabs */}
      <div className="border-b border-gray-800 px-4">
        <div className="flex gap-4">
          <TabButton
            active={activeTab === 'endpoints'}
            onClick={() => setActiveTab('endpoints')}
            label={`Endpoints (${parseResult.endpointCount})`}
          />
          <TabButton
            active={activeTab === 'schemas'}
            onClick={() => setActiveTab('schemas')}
            label={`Schemas (${parseResult.schemaCount})`}
          />
          <TabButton
            active={activeTab === 'raw'}
            onClick={() => setActiveTab('raw')}
            label="Raw Data"
          />
        </div>
      </div>

      {/* Content */}
      <div className="p-4 max-h-[500px] overflow-y-auto scrollbar-thin">
        {activeTab === 'endpoints' && (
          <div className="space-y-2">
            {parseResult.endpoints?.map((ep, i) => (
              <div key={i} className="flex items-center gap-3 px-4 py-3 bg-gray-800/50 rounded-lg">
                <MethodBadge method={ep.method} />
                <span className="font-mono text-sm text-gray-300 flex-1">{ep.path}</span>
                <span className="text-xs text-gray-500">{ep.summary || ep.operationId}</span>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'schemas' && (
          <div className="space-y-4">
            {parseResult.schemas?.map((schema, i) => (
              <div key={i} className="bg-gray-800/50 rounded-lg p-4">
                <h4 className="font-semibold text-gray-100 mb-2">{schema.name}</h4>
                {schema.description && (
                  <p className="text-sm text-gray-500 mb-3">{schema.description}</p>
                )}
                <div className="space-y-1">
                  {schema.fields?.map((field, j) => (
                    <div key={j} className="flex items-center gap-2 text-sm">
                      <span className="font-mono text-indigo-400">{field.name}</span>
                      <span className="text-gray-600">:</span>
                      <span className="font-mono text-gray-400">{field.javaType || field.type}</span>
                      {field.required && (
                        <span className="px-1.5 py-0.5 bg-red-500/15 text-red-400 text-xs rounded">required</span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'raw' && (
          <pre className="text-xs font-mono text-gray-400 whitespace-pre-wrap">
            {JSON.stringify(parseResult, null, 2)}
          </pre>
        )}
      </div>
    </div>
  )
}

function TabButton({ active, onClick, label }) {
  return (
    <button
      onClick={onClick}
      className={`py-3 px-1 text-sm font-medium border-b-2 transition-colors ${
        active
          ? 'border-indigo-500 text-indigo-400'
          : 'border-transparent text-gray-500 hover:text-gray-300'
      }`}
    >
      {label}
    </button>
  )
}

function MethodBadge({ method }) {
  const colors = {
    GET: 'bg-green-500/15 text-green-400',
    POST: 'bg-blue-500/15 text-blue-400',
    PUT: 'bg-yellow-500/15 text-yellow-400',
    DELETE: 'bg-red-500/15 text-red-400',
    PATCH: 'bg-purple-500/15 text-purple-400',
  }

  return (
    <span className={`px-2 py-1 rounded text-xs font-bold ${colors[method] || 'bg-gray-800 text-gray-400'}`}>
      {method}
    </span>
  )
}

export default CodePreview
