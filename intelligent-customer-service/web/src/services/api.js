import axios from 'axios'

/**
 * API服务模块
 * 负责与后端WorkflowController进行通信
 */

// 创建axios实例
const apiClient = axios.create({
  baseURL: '/api/', // 通过Vite代理转发到后端
  timeout: 30000, // 30秒超时
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求拦截器
 * 添加请求日志和错误处理
 */
apiClient.interceptors.request.use(
  (config) => {
    console.log('发送API请求:', config.method?.toUpperCase(), config.url, config.data)
    return config
  },
  (error) => {
    console.error('请求配置错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * 统一处理响应和错误
 */
apiClient.interceptors.response.use(
  (response) => {
    console.log('API响应:', response.status, response.data)
    return response
  },
  (error) => {
    console.error('API请求失败:', error.response?.status, error.response?.data || error.message)
    
    // 根据错误状态码提供友好的错误信息
    let errorMessage = '网络请求失败'
    
    if (error.response) {
      switch (error.response.status) {
        case 400:
          errorMessage = '请求参数错误'
          break
        case 401:
          errorMessage = '未授权访问'
          break
        case 403:
          errorMessage = '访问被禁止'
          break
        case 404:
          errorMessage = '服务不存在'
          break
        case 500:
          errorMessage = '服务器内部错误'
          break
        case 502:
          errorMessage = '网关错误'
          break
        case 503:
          errorMessage = '服务暂时不可用'
          break
        default:
          errorMessage = `请求失败 (${error.response.status})`
      }
    } else if (error.request) {
      errorMessage = '网络连接失败，请检查网络设置'
    }
    
    return Promise.reject(new Error(errorMessage))
  }
)

/**
 * 发送工作流请求
 * 调用后端WorkflowController的处理接口
 * 
 * @param {Object} params - 请求参数
 * @param {string} params.userId - 用户ID (对应userId)
 * @param {string} params.message - 用户消息 (对应userInput)
 * @returns {Promise<string>} 返回AI助手的回复
 */
export const sendWorkflowRequest = async ({ userId, message }) => {
  try {
    // 验证必需参数
    if (!userId || !message) {
      throw new Error('userId和message参数不能为空')
    }

    // 发送POST请求到工作流处理接口
    // 根据WorkflowRunCMD的结构，使用conversationId和userInput字段
    const response = await apiClient.post('/workflows/run', {
      userId: userId.toString(),
      userInput: message.toString()
    })

    // 提取响应数据
    const result = response.data
    
    // 根据后端返回的R<String>结构处理响应
    if (result && result.success && result.data) {
      return result.data
    } else if (result && typeof result.data === 'string') {
      return result.data
    } else if (typeof result === 'string') {
      return result
    } else {
      console.warn('未知的响应格式:', result)
      return result?.message || '收到回复，但格式异常'
    }

  } catch (error) {
    console.error('工作流请求失败:', error)
    throw error
  }
}

/**
 * 获取订单列表
 * 
 * @returns {Promise<Array>} 返回订单列表
 */
export const getOrderList = async () => {
  try {
    const response = await apiClient.post('/order/list')
    return response.data || []
  } catch (error) {
    console.error('获取订单列表失败:', error)
    throw error
  }
}

/**
 * 完成订单
 * 
 * @param {string} orderId - 订单ID
 * @returns {Promise<string>} 返回操作结果
 */
export const completeOrder = async (orderNumber) => {
  try {
    const response = await apiClient.post('/order/complete', { orderNumber })
    return response.data
  } catch (error) {
    console.error('完成订单失败:', error)
    throw error
  }
}

/**
 * 取消订单
 * 
 * @param {string} orderId - 订单ID
 * @returns {Promise<string>} 返回操作结果
 */
export const cancelOrder = async (orderNumber) => {
  try {
    const response = await apiClient.post('/order/cancel', { orderNumber })
    return response.data
  } catch (error) {
    console.error('取消订单失败:', error)
    throw error
  }
}

/**
 * 注册新用户
 * @param {Object} params - 请求参数
 * @param {string} params.name - 用户名
 * @param {string} params.phone - 手机号
 * @returns {Promise<string>} 返回用户ID
 */
export const registerUser = async ({ name, phone }) => {
  try {
    if (!name || !phone) {
      throw new Error('用户名和手机号不能为空');
    }
    const response = await apiClient.post('/user/register', { name, phone });
    console.log('用户注册成功:', response.data);
    return response.data;
  } catch (error) {
    console.error('用户注册失败:', error);
    throw error;
  }
};

// 导出axios实例，供其他模块使用
export default apiClient