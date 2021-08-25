/*******************************************************************************
 * Copyright (c) 2019, 2021 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
import { useMutation } from '@apollo/client';
import IconButton from '@material-ui/core/IconButton';
import Snackbar from '@material-ui/core/Snackbar';
import CloseIcon from '@material-ui/icons/Close';
import { useMachine } from '@xstate/react';
import { DROP_SOURCE_TYPE } from 'common/drop';
import PropTypes from 'prop-types';
import React, { useEffect } from 'react';
import { v4 as uuid } from 'uuid';
import styles from './DropArea.module.css';
import {
  DropAreaContext,
  dropAreaMachine,
  DropEvent,
  HandleDropEvent,
  HandleResponseEvent,
  HideToastEvent,
  SchemaValue,
  ShowToastEvent,
} from './DropAreaMachine';
import { dropOnDiagramMutation } from './operations';

const DATA_DIAGRAM_ID = 'data-diagramid';

const DATA_NODE_ID = 'data-nodeid';

const SVG_NAMESPACE = 'http://www.w3.org/2000/svg';

const propTypes = {
  editingContextId: PropTypes.string.isRequired,
  representationId: PropTypes.string.isRequired,
  invokeHover: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
};

export const DropArea = ({ editingContextId, representationId, invokeHover, children }) => {
  const [{ value, context }, dispatch] = useMachine<DropAreaContext, DropEvent>(dropAreaMachine);
  const { toast } = value as SchemaValue;
  const { message } = context;
  const [dropMutation, { loading, data, error }] = useMutation(dropOnDiagramMutation);

  useEffect(() => {
    if (!loading) {
      if (error) {
        const showToastEvent: ShowToastEvent = {
          type: 'SHOW_TOAST',
          message: 'An unexpected error has occurred, please refresh the page',
        };
        dispatch(showToastEvent);
      }
      if (data) {
        const handleResponseEvent: HandleResponseEvent = { type: 'HANDLE_RESPONSE', data };
        dispatch(handleResponseEvent);

        const typename = data.dropOnDiagram.__typename;
        if (typename === 'ErrorPayload') {
          const { message } = data.dropOnDiagram;
          const showToastEvent: ShowToastEvent = { type: 'SHOW_TOAST', message };
          dispatch(showToastEvent);
        }
      }
    }
  }, [loading, error, data, dispatch]);

  const dropElement = (objectId: string, diagramElementId?: string) => {
    const input = {
      id: uuid(),
      editingContextId,
      representationId,
      objectId,
    };
    if (diagramElementId) {
      input['diagramTargetElementId'] = diagramElementId;
    }

    const handleDropEvent: HandleDropEvent = { type: 'HANDLE_DROP' };
    dispatch(handleDropEvent);
    dropMutation({ variables: { input } });
  };

  const searchId = (dom) => {
    if (dom.hasAttribute(DATA_DIAGRAM_ID)) {
      return dom.getAttribute(DATA_DIAGRAM_ID);
    } else if (dom.hasAttribute(DATA_NODE_ID)) {
      return dom.getAttribute(DATA_NODE_ID);
    } else if (dom.parentElement) {
      const parentDom = dom.parentElement;
      if (parentDom.namespaceURI === SVG_NAMESPACE) {
        return searchId(parentDom);
      }
    }
    return null;
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    const targetId = searchId(e.target);
    // use a standard array instead of a DataTransferItemList
    const dataTransferItems = [...e.dataTransfer.items];
    const sourceItem = dataTransferItems.find((item) => item.type !== DROP_SOURCE_TYPE);
    if (sourceItem) {
      // Update the cursor thanks to dropEffect (a drag'n'drop cursor does not use CSS rules)
      e.dataTransfer.dropEffect = 'link';
    }
    invokeHover(targetId, true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    const id = searchId(e.target);
    invokeHover(id, false);
  };

  const handleDrop = (e) => {
    e.preventDefault();

    const dragSourceStringified = e.dataTransfer.getData(DROP_SOURCE_TYPE);
    if (dragSourceStringified) {
      const source = JSON.parse(dragSourceStringified);
      if (source?.id && source?.kind) {
        const { id: sourceId } = source;
        const diagramElementId = searchId(e.target);
        if (diagramElementId) {
          dropElement(sourceId, diagramElementId);
        } else {
          dropElement(sourceId);
        }
      }
    }
  };

  return (
    <div
      className={styles.dropArea}
      onDrop={(e) => handleDrop(e)}
      onDragOver={(e) => handleDragOver(e)}
      onDragLeave={(e) => handleDragLeave(e)}>
      <Snackbar
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        open={toast === 'visible'}
        autoHideDuration={3000}
        onClose={() => dispatch({ type: 'HIDE_TOAST' } as HideToastEvent)}
        message={message}
        action={
          <IconButton
            size="small"
            aria-label="close"
            color="inherit"
            onClick={() => dispatch({ type: 'HIDE_TOAST' } as HideToastEvent)}>
            <CloseIcon fontSize="small" />
          </IconButton>
        }
        data-testid="error"
      />
      {children}
    </div>
  );
};
DropArea.propTypes = propTypes;
