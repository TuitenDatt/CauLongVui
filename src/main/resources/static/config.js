/**
 * config.js — Cấu hình môi trường cho CauLongVui frontend.
 *
 * Khi chạy LOCAL: API calls về cùng origin (http://localhost:8080)
 * Khi PRODUCTION (S3+CloudFront): thay cdnUrl thành CloudFront domain của bạn.
 *
 * HƯỚNG DẪN ĐỔI PRODUCTION:
 * Thay dòng apiBase thành URL backend (EC2 hoặc CloudFront behavior /api/*):
 *   apiBase: 'https://xxx.cloudfront.net/api'
 * Thay cdnUrl thành CloudFront domain:
 *   cdnUrl: 'https://xxx.cloudfront.net'
 */
(function () {
  // Phát hiện môi trường tự động dựa trên hostname
  const isLocal = window.location.hostname === 'localhost'
    || window.location.hostname === '127.0.0.1';

  window.APP_CONFIG = {
    // Base URL cho tất cả API calls (/api/...)
    // Khi local: '' (same-origin), khi production: https://cloudfront-domain/api
    apiBase: isLocal ? '' : 'https://d33wcfjdygo2at.cloudfront.net',

    // CloudFront CDN URL (dùng cho ảnh upload)
    cdnUrl: isLocal ? '' : 'https://d33wcfjdygo2at.cloudfront.net',
  };
})();
