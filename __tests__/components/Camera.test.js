import React from 'react';
import { NativeModules } from 'react-native';
import { act, render } from '@testing-library/react-native';

import { Camera, UserTrackingMode } from '../../src/components/Camera';

const coordinate1 = [-111.8678, 40.2866];

const bounds1 = {
  ne: [-74.12641, 40.797968],
  sw: [-74.143727, 40.772177],
};

const toFeature = (position) => {
  return {
    type: 'Feature',
    geometry: {
      type: 'Point',
      coordinates: position,
    },
    properties: {},
  };
};

const toFeatureCollection = (bounds) => {
  return {
    type: 'FeatureCollection',
    features: [toFeature(bounds.ne), toFeature(bounds.sw)],
  };
};
describe('Camera', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('defaults are set', () => {
    const result = render(<Camera />);
    const { props } = result.queryByTestId('Camera');
    expect(props.stop).toStrictEqual(null);
  });
  test('set location by center', () => {
    const result = render(
      <Camera centerCoordinate={coordinate1} zoomLevel={14} />,
    );
    const { props } = result.queryByTestId('Camera');
    props.stop.centerCoordinate = JSON.parse(props.stop.centerCoordinate);
    expect(props.stop).toStrictEqual({
      centerCoordinate: toFeature(coordinate1),
      zoom: 14,
    });
  });
  test('set location by bounds', () => {
    const result = render(<Camera bounds={bounds1} />);
    const { props } = result.queryByTestId('Camera');
    props.stop.bounds = JSON.parse(props.stop.bounds);
    expect(props.stop).toStrictEqual({
      bounds: toFeatureCollection(bounds1),
    });
  });
  test('animation mode', () => {
    const result = render(<Camera bounds={bounds1} animationMode={'moveTo'} />);
    const { props } = result.queryByTestId('Camera');
    expect(props.stop.mode).toEqual('Move');
  });

  test('imperatively updates follow configuration', async () => {
    const camera = React.createRef();
    render(<Camera ref={camera} />);

    await act(async () => {
      camera.current.setCamera({
        centerCoordinate: coordinate1,
        followUserLocation: true,
        followUserMode: UserTrackingMode.FollowWithCourse,
        followZoomLevel: 15,
        followPitch: 30,
        followPadding: {
          paddingTop: 10,
          paddingBottom: 20,
        },
      });
    });

    expect(
      NativeModules.RNMBXCameraModule.updateCameraFollowConfig,
    ).toHaveBeenCalledWith(expect.any(Number), {
      followUserLocation: true,
      followUserMode: UserTrackingMode.FollowWithCourse,
      followZoomLevel: 15,
      followPitch: 30,
      followPadding: {
        paddingTop: 10,
        paddingBottom: 20,
      },
    });
    expect(
      NativeModules.RNMBXCameraModule.updateCameraStop,
    ).not.toHaveBeenCalled();
  });

  test('can stop following and update the camera in one call', async () => {
    const camera = React.createRef();
    render(<Camera ref={camera} followUserLocation />);

    await act(async () => {
      camera.current.setCamera({
        centerCoordinate: coordinate1,
        followUserLocation: false,
      });
    });

    expect(
      NativeModules.RNMBXCameraModule.updateCameraFollowConfig,
    ).toHaveBeenCalledWith(expect.any(Number), {
      followUserLocation: false,
    });
    expect(
      NativeModules.RNMBXCameraModule.updateCameraStop,
    ).toHaveBeenCalledWith(expect.any(Number), {
      centerCoordinate: expect.any(String),
    });
    const [, stop] =
      NativeModules.RNMBXCameraModule.updateCameraStop.mock.calls[0];
    expect(JSON.parse(stop.centerCoordinate)).toStrictEqual(
      toFeature(coordinate1),
    );
  });
});
