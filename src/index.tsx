import NativePdfAnnotationView, {
    Commands,
    type NativeProps,
} from './PdfAnnotationViewNativeComponent';
import { forwardRef, useImperativeHandle, useRef } from 'react';
import { Platform } from 'react-native';
export * from './PdfAnnotationViewNativeComponent';

type ComponentRef = InstanceType<typeof NativePdfAnnotationView>;

export interface Handle {
    saveAnnotations: (file: string) => void;
    loadAnnotations: (file: string) => void;
    undo: () => void;
    redo: () => void;
    clear: () => void;
}

export const PdfAnnotationView = forwardRef<Handle, NativeProps>(
    ({ brushSettings, ...props }, ref) => {
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
            undo() {
                if (!nativeRef.current) {
                    return;
                }
                Commands.undo(nativeRef.current);
            },
            redo() {
                if (!nativeRef.current) {
                    return;
                }
                Commands.redo(nativeRef.current);
            },
            clear() {
                if (!nativeRef.current) {
                    return;
                }
                Commands.clear(nativeRef.current);
            },
        }));

        return (
            <NativePdfAnnotationView
                {...props}
                ref={nativeRef}
                brushSettings={Platform.select({
                    ios: brushSettings ?? {
                        type: 'none',
                        color: '#000000',
                        size: 0,
                    },
                    default: brushSettings,
                })}
            />
        );
    }
);
