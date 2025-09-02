import React, { useState } from 'react';
import { apiClient } from '@/lib/api';

const Home: React.FC = () => {
  const [healthStatus, setHealthStatus] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const handleHealthCheck = async () => {
    setIsLoading(true);
    setHealthStatus('');
    
    try {
      const response = await apiClient.get('/health');
      console.log('Response:', response);
      setHealthStatus('✅ 백엔드 연결 성공!');
    } catch (error) {
      console.error('Error:', error);
      setHealthStatus('❌ 백엔드 연결 실패');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="max-w-4xl mx-auto px-4 text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-6">
          Frontend Skeleton
        </h1>
        <p className="text-xl text-gray-600 mb-8">
          Oboe Vintage 프론트엔드 스켈레톤
        </p>
        
        <div className="bg-white p-8 rounded-lg shadow-md mb-8">
          <h2 className="text-2xl font-semibold mb-4">백엔드 헬스체크</h2>
          <button
            onClick={handleHealthCheck}
            disabled={isLoading}
            className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {isLoading ? '체크 중...' : '헬스체크 실행'}
          </button>
          {healthStatus && (
            <p className="mt-4 text-lg font-medium">{healthStatus}</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default Home;
