import React, { useRef, useState } from 'react';
import {
    StyleSheet,
    TouchableOpacity,
    View,
    Text,
    Modal,
    FlatList,
    ActivityIndicator,
    Animated,
    useWindowDimensions,
} from 'react-native';
import {tags} from "../api/OllamaApi.ts";
import Icon from "react-native-vector-icons/MaterialIcons";
import {useAppTheme} from "../theme/ThemeContext.tsx";
import { List } from 'react-native-paper';
import {useTranslation} from "react-i18next";
import {logger} from "../utils/LogUtils.ts";

const ModelSelector = ({ onModelSelect = (model: OllamaModel) => {}, currentModel = 'AI Assistant' }) => {
    const theme = useAppTheme();
    const { t, i18n } = useTranslation();
    const log = logger.createModuleLogger('ModelSelector');
    const [visible, setVisible] = useState(false);
    const [models, setModels] = useState<OllamaModel[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // Animation value for rotate transform（useRef 保证只创建一次，重渲染复用同一节点，
    // 否则每次渲染 new Animated.Value 会生成新原生节点，导致
    // "Animated node does not exist" 崩溃——点击设置触发 HomePage 重渲染时必现）
    const rotateAnimation = useRef(new Animated.Value(0)).current;
    const { width: windowWidth } = useWindowDimensions();

    const toggleDropdown = () => {
        setVisible(!visible);
        Animated.timing(rotateAnimation, {
            toValue: visible ? 0 : 1,
            duration: 200,
            useNativeDriver: true
        }).start();

        if (!visible) {
            fetchModels();
        }
    };

    const fetchModels = async () => {
        setLoading(true);
        setError("");

        await tags()
            .then((response) => {
                setModels(response.models);
            })
            .catch((err)=>{
                setError(t('loadModelFailed'));
                log.error(`Get model list error: ${err}`)
            })
            .finally(()=>{
                setLoading(false);
            })
    };

    const handleModelSelect = (model: OllamaModel) => {
        onModelSelect(model);
        setVisible(false);
        rotateAnimation.setValue(0);
    };

    const renderModel = ({ item }: { item: OllamaModel }) => (
        <List.Item
            title={item.name}
            titleStyle={styles.text}
            style={styles.modelItem}
            onPress={() => handleModelSelect(item)}
        />
    );

    const styles = StyleSheet.create({
        header: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 12,
        },
        headerText: {
            fontSize: 18,
            fontWeight: '600',
            marginRight: 8,
            color: theme.colors.onSurface,
            position: 'relative',
            maxWidth: windowWidth / 2,
        },
        arrow: {
            fontSize: 14,
        },
        modalOverlay: {
            flex: 1,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            justifyContent: 'flex-start',
        },
        dropdownContainer: {
            backgroundColor: 'white',
            marginTop: 50,
            marginHorizontal: 20,
            borderRadius: 10,
            maxHeight: '50%',
            shadowOffset: {
                width: 0,
                height: 2,
            },
            shadowOpacity: 0.25,
            shadowRadius: 3.84,
            elevation: 5,
        },
        modelList: {
            flexGrow: 0,
            backgroundColor: theme.colors.surface,
            borderRadius: 10
        },
        modelItem: {
            padding: 15,
            borderBottomWidth: StyleSheet.hairlineWidth,
            borderBottomColor: theme.colors.primaryContainer,
        },
        loadingContainer: {
            padding: 20,
            alignItems: 'center',
            backgroundColor: theme.colors.surface,
            borderRadius: 10
        },
        loadingText: {
            marginTop: 10,
            color: theme.colors.onSurface,
        },
        errorContainer: {
            backgroundColor: theme.colors.errorContainer,
            borderRadius: 10,
            padding: 20,
            alignItems: 'center',
        },
        errorText: {
            color: theme.colors.error,
        },
        text: {
            color: theme.colors.onSurface
        }
    });

    return (
        <View>
            <TouchableOpacity
                style={styles.header}
                onPress={toggleDropdown}
                activeOpacity={0.7}
            >
                <Text
                    style={styles.headerText}
                    numberOfLines={1}
                    ellipsizeMode='tail'
                >{currentModel}
                </Text>
                <Icon
                    name="keyboard-arrow-down"
                    size={24}
                    color={theme.colors.onSurface}
                />
            </TouchableOpacity>

            <Modal
                visible={visible}
                transparent={true}
                animationType="fade"
                onRequestClose={toggleDropdown}
            >
                <TouchableOpacity
                    style={styles.modalOverlay}
                    activeOpacity={1}
                    onPress={toggleDropdown}
                >
                    <View style={styles.dropdownContainer}>
                        {loading ? (
                            <View style={styles.loadingContainer}>
                                <ActivityIndicator size="small" color="#0066cc" />
                                <Text style={styles.loadingText}>Loading models...</Text>
                            </View>
                        ) : error ? (
                            <View style={styles.errorContainer}>
                                <Text style={styles.errorText}>{error}</Text>
                            </View>
                        ) : (
                            <FlatList
                                data={models}
                                renderItem={renderModel}
                                keyExtractor={(_, index) => index.toString()}
                                bounces={false}
                                showsVerticalScrollIndicator={true}
                                style={styles.modelList}
                            />
                        )}
                    </View>
                </TouchableOpacity>
            </Modal>
        </View>
    );
};

export default ModelSelector;
