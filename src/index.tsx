import NativePdfAnnotationView, {
    Commands,
    type NativeProps,
} from './PdfAnnotationViewNativeComponent';
import { forwardRef, useImperativeHandle, useRef } from 'react';
export * from './PdfAnnotationViewNativeComponent';

type ComponentRef = InstanceType<typeof NativePdfAnnotationView>;

export interface Handle {
    saveAnnotations: (file: string) => void;
    loadAnnotations: (file: string) => void;
}

export const PdfAnnotationView = forwardRef<Handle, NativeProps>(
    (props, ref) => {
        const nativeRef = useRef<ComponentRef>(null);

        useImperativeHandle(ref, () => ({
            saveAnnotations(file) {
                if (!nativeRef.current) {
                    return;
                }
                Commands.saveAnnotations(nativeRef.current, file);
            },
            loadAnnotations(file) {
                if (!nativeRef.current) {
                    return;
                }
                Commands.loadAnnotations(nativeRef.current, file);
            },
        }));

        return <NativePdfAnnotationView {...props} ref={nativeRef} />;
    }
);
