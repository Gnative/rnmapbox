# Contours

This forked version adds serveral updates needed for Contours

1. Offline Legacy - observers (progress, error etc)
2. Camera padding fixes
3. [Performance] Reduces Location updates
4. [Performance] Disable Heading updates
5. [Performance] Adds a cache for JSON Shapes to prevent continuous JSON string processing to native level
6. [Performance] Ability to pause location updates to the native map layer to prevent rendering the Mapbox view while we are transitioning screens or other reanimated effects

# Publishing

Ensure you have an `.npmrc` file with credentials.

1. Update package.json name 
2. `npm publish`
