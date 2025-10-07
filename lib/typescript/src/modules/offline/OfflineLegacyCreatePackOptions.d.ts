export type OfflineLegacyCreatePackOptionsArgs = {
    name: string;
    styleURL: string;
    bounds: [GeoJSON.Position, GeoJSON.Position];
    minZoom?: number;
    maxZoom?: number;
    metadata?: Record<string, unknown>;
};
declare class OfflineLegacyCreatePackOptions {
    readonly name: string;
    readonly styleURL: string;
    readonly bounds: string;
    readonly minZoom: number | undefined;
    readonly maxZoom: number | undefined;
    readonly metadata: string | undefined;
    constructor(options: OfflineLegacyCreatePackOptionsArgs);
    _assert(options: OfflineLegacyCreatePackOptionsArgs): void;
    _makeLatLngBounds(bounds: [GeoJSON.Position, GeoJSON.Position]): string;
    _makeMetadata(metadata: Record<string, unknown> | undefined): string;
}
export default OfflineLegacyCreatePackOptions;
//# sourceMappingURL=OfflineLegacyCreatePackOptions.d.ts.map