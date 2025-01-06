import {
    View,
    StyleSheet,
    Alert,
    Button,
    TouchableHighlight,
    Text,
    SafeAreaView,
} from 'react-native';
import {
    type BrushSettings,
    type Handle,
    PdfAnnotationView,
} from 'react-native-pdf-annotation';
import { type ReactNode, useRef, useState } from 'react';
import DocumentPicker, { types } from 'react-native-document-picker';
import {
    CheckIcon,
    HighlighterIcon,
    PencilIcon,
    PenIcon,
    XIcon,
} from 'lucide-react-native';

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

const getAnnotationsPath = (file: string | null) => {
    if (!file) return null;
    const filename = file
        .substring(file.lastIndexOf('/') + 1, file.length)
        .replace('.pdf', '.ant');
    const folder = file.substring(
        0,
        file.substring(0, file.lastIndexOf('/')).lastIndexOf('/')
    );
    return folder + '/' + filename;
};

export default function App() {
    const [pdfFile, setPdfFile] = useState<string | null>(null);
    const [brush, setBrush] = useState<BrushSettings | undefined>(undefined);
    const [thumbnail, setThumbnail] = useState(false);
    const pdfViewer = useRef<Handle>(null);
    const annotationFile = getAnnotationsPath(pdfFile);

    const handleSelectFile = async () => {
        try {
            const response = await DocumentPicker.pickSingle({
                type: types.pdf,
                copyTo: 'documentDirectory',
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
        if (!annotationFile) return;
        pdfViewer.current?.saveAnnotations(annotationFile);
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.topBar}>
                <Button title={'Select PDF'} onPress={handleSelectFile} />
                {pdfFile && (
                    <>
                        <Button
                            title={'Save Annotations'}
                            onPress={handleSaveAnnotations}
                        />
                        <TouchableHighlight
                            onPress={() => setThumbnail((o) => !o)}
                        >
                            <View style={styles.thumbnailButton}>
                                <Text>Thumbnail: </Text>
                                {thumbnail ? <CheckIcon /> : <XIcon />}
                            </View>
                        </TouchableHighlight>
                    </>
                )}
            </View>
            {pdfFile && (
                <>
                    <PdfAnnotationView
                        pdfUrl={pdfFile}
                        annotationFile={annotationFile ?? undefined}
                        style={styles.box}
                        brushSettings={brush}
                        ref={pdfViewer}
                        thumbnailMode={thumbnail}
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
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    topBar: {
        flexDirection: 'row',
        padding: 6,
        justifyContent: 'space-between',
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
    thumbnailButton: {
        padding: 6,
        flexDirection: 'row',
        gap: 2,
        backgroundColor: 'lightgray',
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
