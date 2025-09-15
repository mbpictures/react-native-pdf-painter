import { codegenNativeComponent } from 'react-native';
import type { CodegenTypes, HostComponent, ViewProps } from 'react-native';
import React from 'react';
import { codegenNativeCommands } from 'react-native';

export type BrushType =
    | 'marker'
    | 'pressure-pen'
    | 'highlighter'
    | 'eraser'
    | 'link'
    | 'none';

export interface BrushSettings {
    type?: CodegenTypes.WithDefault<BrushType, 'marker'>;
    color: string;
    size: CodegenTypes.Float;
}

export type PageCountEvent = {
    pageCount: CodegenTypes.Int32;
};

export type PageChangeEvent = {
    currentPage: CodegenTypes.Int32;
};

export type DocumentFinishedEvent = {
    next: boolean;
};
export type TapEvent = {
    x: CodegenTypes.Double;
    y: CodegenTypes.Double;
};
export type LinkCompletedEvent = {
    fromPage: CodegenTypes.Int32;
    toPage: CodegenTypes.Int32;
};

export interface NativeProps extends ViewProps {
    backgroundColor?: string;
    pdfUrl?: string;
    thumbnailMode?: boolean;
    annotationFile?: string;
    autoSave?: boolean;
    brushSettings?: BrushSettings;
    iosToolPickerVisible?: boolean;
    onPageCount?: CodegenTypes.BubblingEventHandler<PageCountEvent> | null;
    onPageChange?: CodegenTypes.BubblingEventHandler<PageChangeEvent> | null;
    onDocumentFinished?: CodegenTypes.BubblingEventHandler<DocumentFinishedEvent> | null;
    onTap?: CodegenTypes.BubblingEventHandler<TapEvent> | null;
    onLinkCompleted?: CodegenTypes.BubblingEventHandler<LinkCompletedEvent> | null;
    androidBeyondViewportPageCount?: CodegenTypes.Int32;
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
    setPage: (
        viewRef: React.ElementRef<ComponentType>,
        page: CodegenTypes.Int32
    ) => void;
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
