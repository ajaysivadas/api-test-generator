import React from 'react'

const standaloneSteps = [
  { key: 'parsing', label: 'Parsing API Spec' },
  { key: 'schemas', label: 'Generating POJOs' },
  { key: 'executors', label: 'Generating API Executors' },
  { key: 'payloads', label: 'Generating Payload Builders' },
  { key: 'tests', label: 'Generating Test Classes' },
  { key: 'framework', label: 'Building Framework' },
  { key: 'packaging', label: 'Packaging ZIP' },
]

const mergeSteps = [
  { key: 'parsing', label: 'Parsing API Spec' },
  { key: 'requestPojos', label: 'Generating Request POJOs' },
  { key: 'responsePojos', label: 'Generating Response POJOs' },
  { key: 'executors', label: 'Generating API Executors' },
  { key: 'payloads', label: 'Generating Payload Builders' },
  { key: 'tests', label: 'Generating Test Classes' },
  { key: 'suite', label: 'Generating TestNG Suite' },
  { key: 'packaging', label: 'Packaging ZIP' },
]

function GenerationProgress({ currentStep, completed, mode = 'standalone' }) {
  const steps = mode === 'merge' ? mergeSteps : standaloneSteps
  const currentIndex = steps.findIndex(s => s.key === currentStep)

  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
      <h3 className="text-lg font-semibold text-gray-100 mb-4">Generation Progress</h3>
      <div className="space-y-3">
        {steps.map((step, index) => {
          const isCompleted = completed || index < currentIndex
          const isCurrent = !completed && index === currentIndex

          return (
            <div key={step.key} className="flex items-center gap-3">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                isCompleted
                  ? 'bg-green-500/15 text-green-400'
                  : isCurrent
                  ? 'bg-indigo-500/15 text-indigo-400'
                  : 'bg-gray-800 text-gray-600'
              }`}>
                {isCompleted ? (
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                ) : isCurrent ? (
                  <div className="w-3 h-3 rounded-full bg-indigo-400 animate-pulse" />
                ) : (
                  <span className="text-sm">{index + 1}</span>
                )}
              </div>
              <span className={`text-sm ${
                isCompleted ? 'text-green-400' : isCurrent ? 'text-indigo-400 font-medium' : 'text-gray-600'
              }`}>
                {step.label}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default GenerationProgress
