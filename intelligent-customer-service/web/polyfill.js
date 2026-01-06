import { webcrypto } from 'crypto';

// 为全局 crypto 添加 getRandomValues 方法
if (typeof global.crypto === 'undefined') {
    global.crypto = webcrypto;
}

// 确保 getRandomValues 方法存在
if (typeof global.crypto.getRandomValues === 'undefined') {
    global.crypto.getRandomValues = webcrypto.getRandomValues.bind(webcrypto);
}