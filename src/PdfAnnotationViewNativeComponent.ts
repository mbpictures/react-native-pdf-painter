import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps } from 'react-native';
import type {
    Float,
    WithDefault,
} from 'react-native/Libraries/Types/CodegenTypes';

export type BrushType = 'marker' | 'pressure-pen' | 'highlighter';

export interface BrushSettings {
    type?: WithDefault<BrushType, 'marker'>;
    color: string;
    size: Float;
}

interface NativeProps extends ViewProps {
    backgroundColor?: string;
    pdfUrl?: string;
    thumbnailMode?: boolean;
    annotationFile?: string;
    autoSave?: boolean;
    brushSettings?: BrushSettings;
    hidePagination?: boolean;
}

export default codegenNativeComponent<NativeProps>('PdfAnnotationView');
