import React, { useState } from 'react'

const categoryConfig = {
  RequestPojo: { label: 'Request POJOs', color: 'bg-blue-500/15 text-blue-400' },
  ResponsePojo: { label: 'Response POJOs', color: 'bg-green-500/15 text-green-400' },
  ApiExecutors: { label: 'API Executors', color: 'bg-purple-500/15 text-purple-400' },
  Payloads: { label: 'Payload Builders', color: 'bg-orange-500/15 text-orange-400' },
  'src/test': { label: 'Test Classes', color: 'bg-red-500/15 text-red-400' },
  'test-suite': { label: 'TestNG Suite', color: 'bg-yellow-500/15 text-yellow-400' },
  MERGE: { label: 'Instructions', color: 'bg-gray-500/15 text-gray-400' },
}

function categorizeFile(filePath) {
  for (const key of Object.keys(categoryConfig)) {
    if (filePath.includes(key)) return key
  }
  return 'MERGE'
}

function GeneratedFileList({ files }) {
  const [expanded, setExpanded] = useState(false)

  const grouped = {}
  for (const file of files) {
    const cat = categorizeFile(file)
    if (!grouped[cat]) grouped[cat] = []
    grouped[cat].push(file)
  }

  const displayFiles = expanded ? files : files.slice(0, 8)

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-semibold text-gray-100">Generated Files ({files.length})</h4>
        {files.length > 8 && (
          <button
            onClick={() => setExpanded(!expanded)}
            className="text-xs text-indigo-400 hover:text-indigo-300"
          >
            {expanded ? 'Show Less' : `Show All (${files.length})`}
          </button>
        )}
      </div>

      {/* Category badges */}
      <div className="flex flex-wrap gap-2">
        {Object.entries(grouped).map(([cat, catFiles]) => {
          const config = categoryConfig[cat] || categoryConfig.MERGE
          return (
            <span key={cat} className={`text-xs px-2 py-1 rounded-full font-medium ${config.color}`}>
              {config.label}: {catFiles.length}
            </span>
          )
        })}
      </div>

      {/* File list */}
      <div className="bg-gray-800/50 rounded-lg p-3 max-h-64 overflow-y-auto">
        <ul className="space-y-1">
          {displayFiles.map((file, index) => {
            const cat = categorizeFile(file)
            const config = categoryConfig[cat] || categoryConfig.MERGE
            const fileName = file.split('/').pop()
            const dirPath = file.substring(0, file.length - fileName.length)

            return (
              <li key={index} className="flex items-center gap-2 text-xs font-mono">
                <span className={`w-2 h-2 rounded-full flex-shrink-0 ${config.color.split(' ')[0]}`} />
                <span className="text-gray-600 truncate">{dirPath}</span>
                <span className="text-gray-300 font-medium">{fileName}</span>
              </li>
            )
          })}
        </ul>
      </div>
    </div>
  )
}

export default GeneratedFileList
