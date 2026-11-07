import type { InjectionKey } from 'vue';

export interface MapTargetRequest {
  label: string;
  options: () => { id: number; label: string }[];
  selected: () => number[];
  select: (id: number) => void;
  multiple?: boolean;
}

export const mapTargetKey: InjectionKey<{ open: (request: MapTargetRequest) => void; close: (request: MapTargetRequest) => void }> = Symbol('mapTarget');
