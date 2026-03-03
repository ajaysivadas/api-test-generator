import React, { useState } from 'react'
import FileUpload from '../components/FileUpload'
import GenerationProgress from '../components/GenerationProgress'
import DownloadButton from '../components/DownloadButton'
import EnumEntriesPreview from '../components/EnumEntriesPreview'
import GeneratedFileList from '../components/GeneratedFileList'
import { generateFramework, mergeIntoFramework, downloadMerge } from '../services/api'

function Generate() {
  const [file, setFile] = useState(null)
  const [mode, setMode] = useState('standalone')
  const [basePackage, setBasePackage] = useState('com.example.api')
  const [serviceName, setServiceName] = useState('')
  const [baseUriKey, setBaseUriKey] = useState('')
  const [baseUriEnumName, setBaseUriEnumName] = useState('')
  const [generating, setGenerating] = useState(false)
  const [currentStep, setCurrentStep] = useState(null)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  const handleGenerate = async () => {
    if (!file) return
    if (mode === 'merge' && (!serviceName.trim() || !baseUriKey.trim())) return

    setGenerating(true)
    setError(null)
    setResult(null)

    if (mode === 'standalone') {
      const steps = ['parsing', 'schemas', 'executors', 'payloads', 'tests', 'framework', 'packaging']
      for (let i = 0; i < steps.length - 1; i++) {
        setCurrentStep(steps[i])
        await new Promise(resolve => setTimeout(resolve, 500))
      }
      setCurrentStep('packaging')

      try {
        const res = await generateFramework(file, basePackage)
        if (res.success) {
          setResult(res)
          setCurrentStep(null)
        } else {
          setError(res.error)
        }
      } catch (err) {
        setError(err.response?.data?.error || err.message)
      } finally {
        setGenerating(false)
      }
    } else {
      const steps = ['parsing', 'requestPojos', 'responsePojos', 'executors', 'payloads', 'tests', 'suite', 'packaging']
      for (let i = 0; i < steps.length - 1; i++) {
        setCurrentStep(steps[i])
        await new Promise(resolve => setTimeout(resolve, 400))
      }
      setCurrentStep('packaging')

      try {
        const res = await mergeIntoFramework(file, serviceName.trim(), baseUriKey.trim(), baseUriEnumName.trim() || null)
        if (res.success) {
          setResult(res)
          setCurrentStep(null)
        } else {
          setError(res.error)
        }
      } catch (err) {
        setError(err.response?.data?.error || err.message)
      } finally {
        setGenerating(false)
      }
    }
  }

  const handleDownloadMerge = () => {
    if (result?.generationId) {
      downloadMerge(result.generationId, serviceName)
    }
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-gray-100">Generate Framework</h2>
        <p className="text-gray-500 mt-1">Upload an API spec to generate a REST Assured + TestNG automation framework</p>
      </div>

      {/* Mode Toggle */}
      <div className="bg-gray-900 rounded-xl border border-gray-800 p-4">
        <label className="block text-sm font-medium text-gray-300 mb-3">Generation Mode</label>
        <div className="flex gap-3">
          <button
            onClick={() => { setMode('standalone'); setResult(null); setError(null) }}
            className={`flex-1 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors ${
              mode === 'standalone'
                ? 'border-indigo-500 bg-indigo-500/10 text-indigo-400'
                : 'border-gray-700 bg-gray-800/50 text-gray-400 hover:border-gray-600'
            }`}
          >
            <div className="font-semibold">Standalone Project</div>
            <div className="text-xs mt-1 opacity-75">Generate a complete new project with pom.xml, BaseTest, etc.</div>
          </button>
          <button
            onClick={() => { setMode('merge'); setResult(null); setError(null) }}
            className={`flex-1 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors ${
              mode === 'merge'
                ? 'border-indigo-500 bg-indigo-500/10 text-indigo-400'
                : 'border-gray-700 bg-gray-800/50 text-gray-400 hover:border-gray-600'
            }`}
          >
            <div className="font-semibold">Merge into Framework</div>
            <div className="text-xs mt-1 opacity-75">Generate code matching your existing framework's patterns</div>
          </button>
        </div>
      </div>

      <FileUpload
        onFileSelect={setFile}
        label="Upload API Specification"
      />

      {file && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6 space-y-4">
          <h3 className="text-lg font-semibold text-gray-100">Configuration</h3>

          {mode === 'standalone' ? (
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Base Package Name
              </label>
              <input
                type="text"
                value={basePackage}
                onChange={(e) => setBasePackage(e.target.value)}
                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-sm text-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 placeholder-gray-600"
                placeholder="com.example.api"
              />
              <p className="text-xs text-gray-600 mt-1">Package structure for generated Java classes</p>
            </div>
          ) : (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Service Name <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={serviceName}
                  onChange={(e) => setServiceName(e.target.value)}
                  className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-sm text-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 placeholder-gray-600"
                  placeholder="e.g. ECommerce"
                />
                <p className="text-xs text-gray-600 mt-1">Used for directory names and class prefixes (PascalCase)</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Base URI Key <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={baseUriKey}
                  onChange={(e) => setBaseUriKey(e.target.value)}
                  className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-sm text-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 placeholder-gray-600"
                  placeholder="e.g. BaseUriECommerce"
                />
                <p className="text-xs text-gray-600 mt-1">Property key used in config.properties for the base URL</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  Base URI Enum Name <span className="text-gray-600">(optional)</span>
                </label>
                <input
                  type="text"
                  value={baseUriEnumName}
                  onChange={(e) => setBaseUriEnumName(e.target.value)}
                  className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-sm text-gray-200 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 placeholder-gray-600"
                  placeholder="e.g. ECommerceService (defaults to Service Name)"
                />
                <p className="text-xs text-gray-600 mt-1">Enum constant name in BaseUri.java (defaults to Service Name)</p>
              </div>
            </div>
          )}

          <button
            onClick={handleGenerate}
            disabled={generating || (mode === 'merge' && (!serviceName.trim() || !baseUriKey.trim()))}
            className="px-6 py-3 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {generating ? 'Generating...' : mode === 'merge' ? 'Generate & Merge' : 'Generate Framework'}
          </button>
        </div>
      )}

      {(generating || currentStep) && (
        <GenerationProgress currentStep={currentStep} completed={!!result} mode={mode} />
      )}

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4">
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}

      {result && mode === 'standalone' && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6 space-y-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-green-500/15 flex items-center justify-center">
              <svg className="w-6 h-6 text-green-400" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
              </svg>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-100">Framework Generated Successfully</h3>
              <p className="text-sm text-gray-500">Your test automation framework is ready for download</p>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
            <div className="bg-gray-800/50 rounded-lg p-4">
              <p className="text-sm text-gray-500">POJOs</p>
              <p className="text-2xl font-bold text-gray-100">{result.pojoCount}</p>
            </div>
            <div className="bg-gray-800/50 rounded-lg p-4">
              <p className="text-sm text-gray-500">Executors</p>
              <p className="text-2xl font-bold text-gray-100">{result.executorCount}</p>
            </div>
            <div className="bg-gray-800/50 rounded-lg p-4">
              <p className="text-sm text-gray-500">Payload Builders</p>
              <p className="text-2xl font-bold text-gray-100">{result.payloadBuilderCount}</p>
            </div>
            <div className="bg-gray-800/50 rounded-lg p-4">
              <p className="text-sm text-gray-500">Test Classes</p>
              <p className="text-2xl font-bold text-gray-100">{result.testCount}</p>
            </div>
            <div className="bg-gray-800/50 rounded-lg p-4">
              <p className="text-sm text-gray-500">Endpoints</p>
              <p className="text-2xl font-bold text-gray-100">{result.endpointCount}</p>
            </div>
          </div>

          <DownloadButton generationId={result.generationId} />
        </div>
      )}

      {result && mode === 'merge' && (
        <div className="space-y-6">
          {/* Success Header + Stats + Download */}
          <div className="bg-gray-900 rounded-xl border border-gray-800 p-6 space-y-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-green-500/15 flex items-center justify-center">
                <svg className="w-6 h-6 text-green-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-100">Merge Files Generated Successfully</h3>
                <p className="text-sm text-gray-500">Follow the steps below to integrate into your framework</p>
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div className="bg-gray-800/50 rounded-lg p-4">
                <p className="text-sm text-gray-500">Request POJOs</p>
                <p className="text-2xl font-bold text-gray-100">{result.requestPojoCount}</p>
              </div>
              <div className="bg-gray-800/50 rounded-lg p-4">
                <p className="text-sm text-gray-500">Response POJOs</p>
                <p className="text-2xl font-bold text-gray-100">{result.responsePojoCount}</p>
              </div>
              <div className="bg-gray-800/50 rounded-lg p-4">
                <p className="text-sm text-gray-500">Test Classes</p>
                <p className="text-2xl font-bold text-gray-100">{result.testClassCount}</p>
              </div>
              <div className="bg-gray-800/50 rounded-lg p-4">
                <p className="text-sm text-gray-500">Payload Builders</p>
                <p className="text-2xl font-bold text-gray-100">{result.payloadBuilderCount}</p>
              </div>
            </div>

            <button
              onClick={handleDownloadMerge}
              className="w-full px-6 py-3 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-500 transition-colors flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              Download Merge ZIP
            </button>
          </div>

          {/* How to Use */}
          <div className="bg-gray-900 rounded-xl border border-gray-800 p-6 space-y-5">
            <h4 className="text-base font-semibold text-gray-100">How to Integrate</h4>

            <div className="space-y-4">
              <div className="flex gap-3">
                <div className="w-7 h-7 rounded-full bg-indigo-500/15 text-indigo-400 flex items-center justify-center flex-shrink-0 text-xs font-bold">1</div>
                <div>
                  <p className="text-sm font-medium text-gray-200">Extract the ZIP into your framework root</p>
                  <p className="text-xs text-gray-500 mt-1">
                    Unzip the downloaded file directly into your <code className="bg-gray-800 px-1 rounded text-gray-400">API-Automation/</code> project root.
                    The ZIP mirrors your framework's directory structure, so all files will land in the correct folders
                    (<code className="bg-gray-800 px-1 rounded text-gray-400">RequestPojo/</code>,
                    <code className="bg-gray-800 px-1 rounded text-gray-400">ResponsePojo/</code>,
                    <code className="bg-gray-800 px-1 rounded text-gray-400">ApiExecutors/</code>, etc.).
                  </p>
                </div>
              </div>

              <div className="flex gap-3">
                <div className="w-7 h-7 rounded-full bg-indigo-500/15 text-indigo-400 flex items-center justify-center flex-shrink-0 text-xs font-bold">2</div>
                <div>
                  <p className="text-sm font-medium text-gray-200">Add the enum entries below to your existing enums</p>
                  <p className="text-xs text-gray-500 mt-1">
                    The generated code references <code className="bg-gray-800 px-1 rounded text-gray-400">BaseUri</code> and
                    <code className="bg-gray-800 px-1 rounded text-gray-400"> EndPoint</code> enum constants that don't exist yet.
                    Copy the entries shown below into your enum files so the project compiles.
                  </p>
                </div>
              </div>

              <div className="flex gap-3">
                <div className="w-7 h-7 rounded-full bg-indigo-500/15 text-indigo-400 flex items-center justify-center flex-shrink-0 text-xs font-bold">3</div>
                <div>
                  <p className="text-sm font-medium text-gray-200">Add the base URL to your config</p>
                  <p className="text-xs text-gray-500 mt-1">
                    In your <code className="bg-gray-800 px-1 rounded text-gray-400">config.properties</code> (or equivalent), add the property:{' '}
                    <code className="bg-gray-800 px-1 rounded text-gray-400">{serviceName ? `${baseUriKey}=https://your-api-url.com` : 'BaseUri=https://your-api-url.com'}</code>
                  </p>
                </div>
              </div>

              <div className="flex gap-3">
                <div className="w-7 h-7 rounded-full bg-indigo-500/15 text-indigo-400 flex items-center justify-center flex-shrink-0 text-xs font-bold">4</div>
                <div>
                  <p className="text-sm font-medium text-gray-200">Update payload builders with real test data</p>
                  <p className="text-xs text-gray-500 mt-1">
                    The generated payload builders use placeholder values (e.g. <code className="bg-gray-800 px-1 rounded text-gray-400">"testEmail"</code>).
                    Replace them with valid test data for your API. Look for <code className="bg-gray-800 px-1 rounded text-gray-400">TODO</code> comments in the generated files.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Enum Entries */}
          {result.endpointEntries && result.endpointEntries.length > 0 && (
            <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
              <EnumEntriesPreview
                endpointEntries={result.endpointEntries}
                baseUriEntry={result.baseUriEntry}
              />
            </div>
          )}

          {/* Generated Files */}
          {result.generatedFiles && result.generatedFiles.length > 0 && (
            <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
              <GeneratedFileList files={result.generatedFiles} />
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default Generate
