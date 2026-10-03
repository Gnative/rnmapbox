const path = require('path');
const pkg = require('../package.json');

// Detect if we're running inside Expo
const isExpo = !!process.env.EXPO_DEV_SERVER_ORIGIN;

const { getDefaultConfig } = isExpo
  ? require('@expo/metro-config')
  : require('@react-native/metro-config');
const { withMetroConfig } = require('react-native-monorepo-config');

const root = path.resolve(__dirname, '..');

/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 *
 * @type {import('metro-config').MetroConfig}
 */
const config = withMetroConfig(getDefaultConfig(__dirname), {
  root,
  dirname: __dirname,
});

// Keep upstream example imports working when this fork uses a different package name.
const resolveRequest = config.resolver.resolveRequest;
config.resolver.resolveRequest = (context, moduleName, platform) =>
  resolveRequest(
    context,
    moduleName === '@rnmapbox/maps' ? pkg.name : moduleName,
    platform,
  );

config.resolver.unstable_enablePackageExports = true;
if (config.resolver.assetExts == null) {
  config.resolver.assetExts = [];
}
config.resolver.assetExts.push('gltf', 'glb', 'png');

module.exports = config;
