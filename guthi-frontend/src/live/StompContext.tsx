import {
  createContext,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { Client } from "@stomp/stompjs";
import { useAuth } from "../auth/AuthContext";
import { wsBrokerConnectUrl } from "../api/client";

type StompContextValue = {
  client: Client | null;
  connected: boolean;
};

const StompContext = createContext<StompContextValue>({ client: null, connected: false });

export function StompProvider({ children }: { children: ReactNode }) {
  const { auth } = useAuth();
  const [client, setClient] = useState<Client | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const token = auth?.token ?? null;
    const c = new Client({
      brokerURL: wsBrokerConnectUrl(token),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });
    setClient(c);
    c.activate();
    return () => {
      setConnected(false);
      c.deactivate();
      setClient(null);
    };
  }, [auth?.token]);

  const value = useMemo(() => ({ client, connected }), [client, connected]);
  return <StompContext.Provider value={value}>{children}</StompContext.Provider>;
}

export function useStomp(): StompContextValue {
  return useContext(StompContext);
}

/** Subscribe while STOMP is connected; cleans up on unmount. Handler always sees latest callback via ref. */
export function useStompSubscription(destination: string | null, onMessage: (body: string) => void) {
  const { client, connected } = useStomp();
  const handlerRef = useRef(onMessage);
  handlerRef.current = onMessage;

  useLayoutEffect(() => {
    if (!destination || !client || !connected) return;
    const sub = client.subscribe(destination, (msg) => handlerRef.current(msg.body));
    return () => {
      try {
        sub.unsubscribe();
      } catch {
        // ignore
      }
    };
  }, [client, connected, destination]);
}
