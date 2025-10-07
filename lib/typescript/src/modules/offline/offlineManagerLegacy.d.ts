import { EventSubscription, NativeEventEmitter } from 'react-native';
export { default as OfflineLegacyCreatePackOptions, type OfflineLegacyCreatePackOptionsArgs, } from './OfflineLegacyCreatePackOptions';
import { type OfflineLegacyCreatePackOptionsArgs } from './OfflineLegacyCreatePackOptions';
import OfflinePackLegacy from './OfflinePackLegacy';
export declare const OfflineModuleEventEmitter: NativeEventEmitter<Readonly<Record<string, readonly Object[]>>>;
export type OfflineProgressStatus = {
    name: string;
    state: number;
    percentage: number;
    completedResourceSize: number;
    completedTileCount: number;
    completedResourceCount: number;
    requiredResourceCount: number;
    completedTileSize: number;
};
export declare enum OfflinePackErrorType {
    NOT_FOUND = 0,
    SERVER = 1,
    CONNECTION = 2,
    RATE_LIMIT = 3,
    DISK_FULL = 4,
    TILE_COUNT_LIMIT_EXCEEDED = 5,
    OTHER = 6
}
export type OfflinePackError = {
    name: string;
    message: string;
    isFatal: boolean;
    type: OfflinePackErrorType;
};
type ErrorEvent = {
    payload: OfflinePackError;
};
type ProgressEvent = {
    payload: OfflineProgressStatus;
};
type ProgressListener = (pack: OfflinePackLegacy, status: OfflineProgressStatus) => void;
type ErrorListener = (pack: OfflinePackLegacy, err: OfflinePackError) => void;
/**
 * OfflineManagerLegacy implements a singleton (shared object) that manages offline packs.
 * All of this class’s instance methods are asynchronous, reflecting the fact that offline resources are stored in a database.
 * The shared object maintains a canonical collection of offline packs.
 */
declare class OfflineManagerLegacy {
    private _hasInitialized;
    private _offlinePacks;
    private _progressListeners;
    private _errorListeners;
    subscriptionProgress: EventSubscription | null;
    subscriptionError: EventSubscription | null;
    constructor();
    /**
     * Creates and registers an offline pack that downloads the resources needed to use the given region offline.
     *
     * @example
     *
     * const progressListener = (offlineRegion, status) => console.log(offlineRegion, status);
     * const errorListener = (offlineRegion, err) => console.log(offlineRegion, err);
     *
     * await Mapbox.offlineManager.createPack({
     *   name: 'offlinePack',
     *   styleURL: 'mapbox://...',
     *   minZoom: 14,
     *   maxZoom: 20,
     *   bounds: [[neLng, neLat], [swLng, swLat]]
     * }, progressListener, errorListener)
     *
     * @param  {OfflineLegacyCreatePackOptions} options Create options for a offline pack that specifices zoom levels, style url, and the region to download.
     * @param  {Callback=} progressListener Callback that listens for status events while downloading the offline resource.
     * @param  {Callback=} errorListener Callback that listens for status events while downloading the offline resource.
     * @return {void}
     */
    createPack(options: OfflineLegacyCreatePackOptionsArgs, progressListener: ProgressListener, errorListener?: ErrorListener): Promise<OfflinePackLegacy>;
    /**
     * Invalidates the specified offline pack. This method checks that the tiles in the specified offline pack match those from the server. Local tiles that do not match the latest version on the server are updated.
     *
     * This is more efficient than deleting the offline pack and downloading it again. If the data stored locally matches that on the server, new data will not be downloaded.
     *
     * @example
     * await Mapbox.offlineManagerLegacy.invalidatePack('packName')
     *
     * @param  {String}  name  Name of the offline pack.
     * @return {void}
     */
    invalidatePack(name: string): Promise<void>;
    /**
     * Unregisters the given offline pack and allows resources that are no longer required by any remaining packs to be potentially freed.
     *
     * @example
     * await Mapbox.offlineManagerLegacy.deletePack('packName')
     *
     * @param  {String}  name  Name of the offline pack.
     * @return {void}
     */
    deletePack(name: string): Promise<void>;
    /**
     * Migrates the offline cache from pre-v10 SDKs to the new v10 cache location
     *
     * @example
     * await Mapbox.offlineManager.migrateOfflineCache()
     *
     * @return {void}
     */
    migrateOfflineCache(): Promise<void>;
    /**
     * Sets the maximum number of Mapbox-hosted tiles that may be downloaded and stored on the current device.
     *
     * @example
     * Mapbox.offlineManager.setTileCountLimit(6000);
     *
     * @param {Number} limit Map tile limit count.
     * @return {void}
     */
    setTileCountLimit(limit: number): void;
    /**
     * Sets the timeout for the pack download when no progress updates have been sent, throws a timeout error
     *
     * @example
     * Mapbox.offlineManager.setTimeout(60);
     *
     * @param {Number} seconds Seconds after last progress update to fire an timeout error
     * @return {void}
     */
    setTimeout(seconds: number): void;
    /**
     * Deletes the existing database, which includes both the ambient cache and offline packs, then reinitializes it.
     *
     * @example
     * await Mapbox.offlineManager.resetDatabase();
     *
     * @return {void}
     */
    resetDatabase(): Promise<void>;
    /**
     * Retrieves all the current offline packs that are stored in the database.
     *
     * @example
     * const offlinePacks = await Mapbox.offlineManagerLegacy.getPacks();
     *
     * @return {Array<OfflinePack>}
     */
    getPacks(): Promise<OfflinePackLegacy[]>;
    /**
     * Retrieves an offline pack that is stored in the database by name.
     *
     * @example
     * const offlinePack = await Mapbox.offlineManagerLegacy.getPack();
     *
     * @param  {String}  name  Name of the offline pack.
     * @return {OfflinePack}
     */
    getPack(name: string): Promise<OfflinePackLegacy | undefined>;
    /**
     * Subscribe to download status/error events for the requested offline pack.
     * Note that createPack calls this internally if listeners are provided.
     *
     * @example
     * const progressListener = (offlinePack, status) => console.log(offlinePack, status)
     * const errorListener = (offlinePack, err) => console.log(offlinePack, err)
     * Mapbox.offlineManager.subscribe('packName', progressListener, errorListener)
     *
     * @param  {String} packName           Name of the offline pack.
     * @param  {Callback} progressListener Callback that listens for status events while downloading the offline resource.
     * @param  {Callback} errorListener      Callback that listens for status events while downloading the offline resource.
     * @return {void}
     */
    subscribe(packName: string, progressListener: ProgressListener, errorListener?: ErrorListener): Promise<void>;
    /**
     * Unsubscribes any listeners associated with the offline pack.
     * It's a good idea to call this on componentWillUnmount.
     *
     * @example
     * Mapbox.offlineManager.unsubscribe('packName')
     *
     * @param  {String} packName Name of the offline pack.
     * @return {void}
     */
    unsubscribe(packName: string): void;
    _initialize(forceInit?: boolean): Promise<boolean>;
    _onProgress(e: ProgressEvent): void;
    _onError(e: ErrorEvent): void;
    _hasListeners(name: string, listenerMap: Record<string, ProgressListener> | Record<string, ErrorListener>): boolean;
}
declare const offlineManagerLegacy: OfflineManagerLegacy;
export default offlineManagerLegacy;
//# sourceMappingURL=offlineManagerLegacy.d.ts.map