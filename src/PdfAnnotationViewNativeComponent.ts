import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { HostComponent, ViewProps } from 'react-native';
import type {
    BubblingEventHandler,
    Double,
    Float,
    Int32,
    WithDefault,
} from 'react-native/Libraries/Types/CodegenTypes';
import React from 'react';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';

export type BrushType =
    | 'marker'
    | 'pressure-pen'
    | 'highlighter'
    | 'eraser'
    | 'none';

export interface BrushSettings {
    type?: WithDefault<BrushType, 'marker'>;
    color: string;
    size: Float;
}

export type PageCountEvent = {
    pageCount: Int32;
};

export type PageChangeEvent = {
    currentPage: Int32;
};

export type DocumentFinishedEvent = {
    next: boolean;
};
export type TapEvent = {
    x: Double;
    y: Double;
};

export interface NativeProps extends ViewProps {
    backgroundColor?: string;
    pdfUrl?: string;
    thumbnailMode?: boolean;
    annotationFile?: string;
    autoSave?: boolean;
    brushSettings?: BrushSettings;
    iosToolPickerVisible?: boolean;
    onPageCount?: BubblingEventHandler<PageCountEvent> | null;
    onPageChange?: BubblingEventHandler<PageChangeEvent> | null;
    onDocumentFinished?: BubblingEventHandler<DocumentFinishedEvent> | null;
    onTap?: BubblingEventHandler<TapEvent> | null;
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
    undo: (viewRef: React.ElementRef<ComponentType>) => void;
    redo: (viewRef: React.ElementRef<ComponentType>) => void;
    clear: (viewRef: React.ElementRef<ComponentType>) => void;
    setPage: (viewRef: React.ElementRef<ComponentType>, page: Int32) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
    supportedCommands: [
        'saveAnnotations',
        'loadAnnotations',
        'undo',
        'redo',
        'clear',
        'setPage',
    ],
});
