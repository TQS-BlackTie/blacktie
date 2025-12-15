module.exports = {
  ci: {
    collect: {
      // Use Vite preview server instead of static serving for proper SPA support
      startServerCommand: "npm run preview -- --port 4173",
      startServerReadyPattern: "Local:",
      url: ["http://localhost:4173"],
      numberOfRuns: 3,
      settings: {
        // Increase timeout for SPA rendering
        maxWaitForLoad: 45000,
        // Chrome flags for CI environment
        chromeFlags: [
          "--no-sandbox",
          "--disable-gpu",
          "--disable-dev-shm-usage",
          "--disable-software-rasterizer",
        ],
      },
    },
    assert: {
      assertions: {
        "categories:performance": ["error", { minScore: 0.80 }],
        "categories:accessibility": ["error", { minScore: 0.90 }],
        "categories:best-practices": ["warn", { minScore: 0.85 }],
        "categories:seo": ["warn", { minScore: 0.85 }],
      },
    },
    upload: {
      target: "temporary-public-storage",
    },
  },
};