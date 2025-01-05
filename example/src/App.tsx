import {
    View,
    StyleSheet,
    Alert,
    Button,
    TouchableHighlight,
} from 'react-native';
import {
    type BrushSettings,
    type Handle,
    PdfAnnotationView,
} from 'react-native-pdf-annotation';
import { type ReactNode, useRef, useState } from 'react';
import DocumentPicker, { types } from 'react-native-document-picker';
import { HighlighterIcon, PencilIcon, PenIcon } from 'lucide-react-native';

const BRUSH_SETTINGS: { settings: BrushSettings; icon: ReactNode }[] = [
    {
        settings: {
            type: 'marker',
            size: 3,
            color: '#000000',
        },
        icon: <PenIcon />,
    },
    {
        settings: {
            type: 'pressure-pen',
            size: 3,
            color: '#000000',
        },
        icon: <PencilIcon />,
    },
    {
        settings: {
            type: 'highlighter',
            size: 15,
            color: '#88FFFF00',
        },
        icon: <HighlighterIcon />,
    },
];

const compareSettings = (a?: BrushSettings, b?: BrushSettings) => {
    return a?.type === b?.type && a?.size === b?.size && a?.color === b?.color;
};

export default function App() {
    const [pdfFile, setPdfFile] = useState<string | null>(null);
    const [brush, setBrush] = useState<BrushSettings | undefined>(undefined);
    const pdfViewer = useRef<Handle>(null);

    const handleSelectFile = async () => {
        try {
            const response = await DocumentPicker.pickSingle({
                type: types.pdf,
                copyTo: 'cachesDirectory',
            });
            setPdfFile(response.fileCopyUri);
        } catch (e) {
            Alert.alert('File Selection Error');
        }
    };

    const handleClickTool = (settings: BrushSettings) => () => {
        setBrush((oldBrush) => {
            if (compareSettings(oldBrush, settings)) {
                return undefined;
            }

            return settings;
        });
    };

    const handleSaveAnnotations = () => {
        if (!pdfFile) return;
        pdfViewer.current?.saveAnnotations(pdfFile?.replace('.pdf', '.ant'));
    };

    return (
        <View style={styles.container}>
            <View style={styles.topBar}>
                <Button title={'Select PDF'} onPress={handleSelectFile} />
                {pdfFile && (
                    <Button
                        title={'Save Annotations'}
                        onPress={handleSaveAnnotations}
                    />
                )}
            </View>
            {pdfFile && (
                <>
                    <PdfAnnotationView
                        pdfUrl={pdfFile}
                        style={styles.box}
                        brushSettings={brush}
                        ref={pdfViewer}
                    />
                    <View style={styles.toolbar}>
                        {BRUSH_SETTINGS.map((config, i) => (
                            <TouchableHighlight
                                key={i}
                                underlayColor={'#ffe9d2'}
                                style={[
                                    styles.toolbarItem,
                                    compareSettings(brush, config.settings) &&
                                        styles.toolbarItemActive,
                                ]}
                                onPress={handleClickTool(config.settings)}
                            >
                                {config.icon}
                            </TouchableHighlight>
                        ))}
                    </View>
                </>
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    topBar: {
        flexDirection: 'row',
    },
    box: {
        flex: 1,
    },
    toolbar: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 10,
        },
        shadowOpacity: 0.39,
        shadowRadius: 4,
        elevation: 13,
    },
    toolbarItem: {
        margin: 6,
        borderRadius: 20,
        borderWidth: 1,
        borderColor: 'black',
        padding: 6,
    },
    toolbarItemActive: {
        backgroundColor: '#ffcc9c',
    },
});
