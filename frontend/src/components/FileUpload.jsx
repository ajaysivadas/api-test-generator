import React, { useCallback } from 'react'
import { useDropzone } from 'react-dropzone'

function FileUpload({ onFileSelect, label = 'Upload API Spec', accept, multiple = false }) {
  const onDrop = useCallback((acceptedFiles) => {
    if (acceptedFiles.length > 0) {
      onFileSelect(multiple ? acceptedFiles : acceptedFiles[0])
    }
  }, [onFileSelect, multiple])

  const { getRootProps, getInputProps, isDragActive, acceptedFiles } = useDropzone({
    onDrop,
    accept: accept || {
      'application/json': ['.json'],
      'application/x-yaml': ['.yaml', '.yml'],
      'text/yaml': ['.yaml', '.yml'],
    },
    multiple,
  })

  return (
    <div
      {...getRootProps()}
      className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all ${
        isDragActive
          ? 'border-indigo-500 bg-indigo-500/10'
          : 'border-gray-700 hover:border-indigo-500/50 hover:bg-gray-800/50'
      }`}
    >
      <input {...getInputProps()} />
      <div className="flex flex-col items-center gap-3">
        <div className="w-12 h-12 rounded-full bg-indigo-500/15 flex items-center justify-center">
          <svg className="w-6 h-6 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
          </svg>
        </div>
        {isDragActive ? (
          <p className="text-indigo-400 font-medium">Drop the file here...</p>
        ) : (
          <>
            <p className="text-gray-200 font-medium">{label}</p>
            <p className="text-sm text-gray-500">
              Drag & drop or click to browse. Supports OpenAPI (YAML/JSON) and Postman Collection (JSON)
            </p>
          </>
        )}
        {acceptedFiles.length > 0 && (
          <div className="mt-2 px-4 py-2 bg-green-500/10 rounded-lg">
            <p className="text-sm text-green-400 font-medium">
              Selected: {acceptedFiles.map(f => f.name).join(', ')}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

export default FileUpload
