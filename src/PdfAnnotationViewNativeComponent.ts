import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { HostComponent, ViewProps } from 'react-native';
import type {
    Float,
    WithDefault,
} from 'react-native/Libraries/Types/CodegenTypes';
import React from 'react';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';

export type BrushType = 'marker' | 'pressure-pen' | 'highlighter';

export interface BrushSettings {
    type?: WithDefault<BrushType, 'marker'>;
    color: string;
    size: Float;
}

export interface NativeProps extends ViewProps {
    backgroundColor?: string;
    pdfUrl?: string;
    thumbnailMode?: boolean;
    annotationFile?: string;
    autoSave?: boolean;
    brushSettings?: BrushSettings;
    hidePagination?: boolean;
}

type ComponentType = HostComponent<NativeProps>;

export default codegenNativeComponent<NativeProps>(
    'PdfAnnotationView'
) as ComponentType;

interface NativeCommands {
    saveAnnotations: (
        viewRef: React.ElementRef<ComponentType>,
        path: string
    ) => void;
    loadAnnotations: (
        viewRef: React.ElementRef<ComponentType>,
        path: string
    ) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
    supportedCommands: ['saveAnnotations', 'loadAnnotations'],
});
