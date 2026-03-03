import React from 'react'

function SchemaChanges({ comparison }) {
  if (!comparison) return null

  const { changes, totalChanges, breakingChanges, nonBreakingChanges } = comparison

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-4">
          <p className="text-sm text-gray-500">Total Changes</p>
          <p className="text-2xl font-bold text-gray-100">{totalChanges}</p>
        </div>
        <div className="bg-gray-900 rounded-xl border border-red-500/30 p-4">
          <p className="text-sm text-red-400">Breaking Changes</p>
          <p className="text-2xl font-bold text-red-400">{breakingChanges}</p>
        </div>
        <div className="bg-gray-900 rounded-xl border border-green-500/30 p-4">
          <p className="text-sm text-green-400">Non-Breaking</p>
          <p className="text-2xl font-bold text-green-400">{nonBreakingChanges}</p>
        </div>
      </div>

      {/* Changes List */}
      <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-800">
          <h3 className="text-lg font-semibold text-gray-100">Change Details</h3>
        </div>
        <div className="divide-y divide-gray-800/50">
          {changes.map((change, index) => (
            <div key={index} className="px-6 py-4 hover:bg-gray-800/30 transition-colors">
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <ChangeTypeBadge type={change.changeType} />
                  <div>
                    <p className="text-sm font-medium text-gray-200">{change.description}</p>
                    <p className="text-xs text-gray-500 mt-1">{change.category} &middot; {change.path}</p>
                    {(change.oldValue || change.newValue) && (
                      <div className="flex gap-4 mt-2 text-xs">
                        {change.oldValue && (
                          <span className="px-2 py-1 bg-red-500/10 text-red-400 rounded font-mono">
                            - {change.oldValue}
                          </span>
                        )}
                        {change.newValue && (
                          <span className="px-2 py-1 bg-green-500/10 text-green-400 rounded font-mono">
                            + {change.newValue}
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                </div>
                <SeverityBadge severity={change.severity} />
              </div>
            </div>
          ))}
          {changes.length === 0 && (
            <div className="px-6 py-12 text-center text-gray-500">
              No changes detected between the two specs
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function ChangeTypeBadge({ type }) {
  const styles = {
    ADDED: 'bg-green-500/15 text-green-400',
    REMOVED: 'bg-red-500/15 text-red-400',
    MODIFIED: 'bg-yellow-500/15 text-yellow-400',
    TYPE_CHANGED: 'bg-purple-500/15 text-purple-400',
  }

  return (
    <span className={`px-2 py-1 rounded text-xs font-medium ${styles[type] || 'bg-gray-800 text-gray-400'}`}>
      {type}
    </span>
  )
}

function SeverityBadge({ severity }) {
  const isBreaking = severity === 'BREAKING'
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
      isBreaking ? 'bg-red-500/15 text-red-400' : 'bg-green-500/15 text-green-400'
    }`}>
      {severity}
    </span>
  )
}

export default SchemaChanges
