import React from 'react';
import Mapbox from '@rnmapbox/maps';
import { AppState, Button, StyleSheet, Text, View } from 'react-native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { NavigationContainer } from '@react-navigation/native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

import sheet from './styles/sheet';
import colors from './styles/colors';
import { IS_ANDROID } from './utils';
import config from './utils/config';
import { Group, Item } from './scenes/GroupAndItem';
import { ScreenWithoutMap } from './scenes/ScreenWithoutMap';
import MapInModal from './examples/Map/MapInModal';

const styles = StyleSheet.create({
  noPermissionsText: {
    fontSize: 18,
    fontWeight: 'bold',
  },
});

Mapbox.addCustomHeader('Custom-Header', 'global-header-value');
Mapbox.addCustomHeader('Mapbox-Api-Header-Value', 'api-header-value', {
  urlRegexp: '^https:\/\/api\.mapbox\.com\/(.*)$',
});
// This header will not be added to requests to api.mapbox.com
Mapbox.addCustomHeader('Other-Api-Header-Value', 'other-api-header-value', {
  urlRegexp: '^https:\/\/api\.other\.com\/(.*)$',
});
Mapbox.setAccessToken(config.get('accessToken'));

console.log('### App.js - Mapbox:', Mapbox);

const Stack = createNativeStackNavigator();

function AppStackNavigator() {
  return (
    <Stack.Navigator
      initialRouteName="Group"
      screenOptions={{ gestureEnabled: false, headerShown: false }}
    >
      <Stack.Screen name="Group" component={Group} />
      <Stack.Screen name="Item" component={Item} />
      <Stack.Screen name="ScreenWithoutMap" component={ScreenWithoutMap} />
      <Stack.Group
        screenOptions={() => ({
          presentation: 'modal',
        })}
      >
        <Stack.Screen name="MapInModal" component={MapInModal} />
      </Stack.Group>
    </Stack.Navigator>
  );
}

const AppContainer = () => (
  <SafeAreaProvider>
    <NavigationContainer>
      <AppStackNavigator />
    </NavigationContainer>
  </SafeAreaProvider>
);
class App extends React.Component {
  // @ts-ignore - Parameter type requires TypeScript annotation
  constructor(props) {
    super(props);

    this.state = {
      isFetchingAndroidPermission: IS_ANDROID,
      isAndroidPermissionGranted: false,
      permissionError: false,
      activeExample: -1,
    };
    this.permissionMounted = false;
    this.permissionRequestInFlight = false;
  }

  componentDidMount() {
    this.permissionMounted = true;
    if (IS_ANDROID) {
      this.permissionSubscription = AppState.addEventListener(
        'change',
        (state) => {
          if (state === 'active' && this.state.isFetchingAndroidPermission) {
            this.requestAndroidPermission();
          }
        },
      );
      this.requestAndroidPermission();
    }
  }

  componentWillUnmount() {
    this.permissionMounted = false;
    this.permissionSubscription?.remove();
  }

  requestAndroidPermission = async () => {
    if (
      !this.permissionMounted ||
      AppState.currentState !== 'active' ||
      this.permissionRequestInFlight ||
      this.state.isAndroidPermissionGranted
    ) {
      return;
    }
    this.permissionRequestInFlight = true;
    this.setState({
      isFetchingAndroidPermission: true,
      permissionError: false,
    });
    try {
      const isGranted = await Mapbox.requestAndroidLocationPermissions();
      if (this.permissionMounted) {
        this.setState({
          isAndroidPermissionGranted: isGranted,
          isFetchingAndroidPermission: false,
        });
      }
    } catch (error) {
      console.warn('Unable to request Android location permission', error);
      if (this.permissionMounted) {
        this.setState({
          isFetchingAndroidPermission: false,
          permissionError: true,
        });
      }
    } finally {
      this.permissionRequestInFlight = false;
    }
  };

  render() {
    if (IS_ANDROID && !this.state.isAndroidPermissionGranted) {
      if (this.state.isFetchingAndroidPermission) {
        return null;
      }
      return (
        <SafeAreaView
          style={[sheet.matchParent, { backgroundColor: colors.primary.blue }]}
        >
          <View style={sheet.matchParent}>
            <Text style={styles.noPermissionsText}>
              {this.state.permissionError
                ? 'Location permission could not be requested. Please retry while the app is open.'
                : 'You need to accept location permissions to use the example app.'}
            </Text>
            <Button
              title="Retry location permission"
              onPress={this.requestAndroidPermission}
            />
          </View>
        </SafeAreaView>
      );
    }
    return <AppContainer />;
  }
}

export default App;
